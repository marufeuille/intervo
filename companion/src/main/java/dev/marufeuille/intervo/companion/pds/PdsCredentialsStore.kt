package dev.marufeuille.intervo.companion.pds

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class PdsCredentials(
    val serviceUrl: String,
    val identifier: String,
    val appPassword: String,
)

data class PdsAccountSettings(
    val serviceUrl: String,
    val identifier: String,
    val hasAppPassword: Boolean,
) {
    val isConfigured: Boolean
        get() = serviceUrl.isNotBlank() && identifier.isNotBlank() && hasAppPassword
}

/**
 * App Password 方式の初期実装用ストア。App Password は Android Keystore の AES-GCM 鍵で暗号化する。
 * OAuth へ移行するときは、このストアを session/token store に差し替える。
 */
class PdsCredentialsStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadSettings(): PdsAccountSettings =
        PdsAccountSettings(
            serviceUrl = prefs.getString(KEY_SERVICE_URL, DEFAULT_SERVICE_URL).orEmpty(),
            identifier = prefs.getString(KEY_IDENTIFIER, "").orEmpty(),
            hasAppPassword = prefs.contains(KEY_PASSWORD_CIPHERTEXT) && prefs.contains(KEY_PASSWORD_IV),
        )

    fun loadCredentials(): PdsCredentials? {
        val settings = loadSettings()
        val password = decryptPassword() ?: return null
        if (settings.serviceUrl.isBlank() || settings.identifier.isBlank() || password.isBlank()) return null
        return PdsCredentials(
            serviceUrl = settings.serviceUrl.normalizedServiceUrl(),
            identifier = settings.identifier.trim(),
            appPassword = password,
        )
    }

    fun save(serviceUrl: String, identifier: String, appPassword: String?) {
        prefs.edit()
            .putString(KEY_SERVICE_URL, serviceUrl.normalizedServiceUrl())
            .putString(KEY_IDENTIFIER, identifier.trim())
            .apply()
        appPassword?.takeIf { it.isNotBlank() }?.let { encryptPassword(it.trim()) }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encryptPassword(password: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_PASSWORD_IV, cipher.iv.base64())
            .putString(KEY_PASSWORD_CIPHERTEXT, ciphertext.base64())
            .apply()
    }

    private fun decryptPassword(): String? = runCatching {
        val iv = prefs.getString(KEY_PASSWORD_IV, null)?.base64Decoded() ?: return null
        val ciphertext = prefs.getString(KEY_PASSWORD_CIPHERTEXT, null)?.base64Decoded() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        const val DEFAULT_SERVICE_URL = "https://pds.marufeuille.dev"

        private const val PREFS = "pds_credentials"
        private const val KEY_SERVICE_URL = "service_url"
        private const val KEY_IDENTIFIER = "identifier"
        private const val KEY_PASSWORD_IV = "password_iv"
        private const val KEY_PASSWORD_CIPHERTEXT = "password_ciphertext"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "intervo_pds_app_password"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128

        fun String.normalizedServiceUrl(): String =
            trim().ifBlank { DEFAULT_SERVICE_URL }.trimEnd('/')

        private fun ByteArray.base64(): String =
            Base64.encodeToString(this, Base64.NO_WRAP)

        private fun String.base64Decoded(): ByteArray =
            Base64.decode(this, Base64.NO_WRAP)
    }
}
