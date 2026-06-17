package dev.marufeuille.intervo.companion.pds

import android.util.Log
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * App Password 方式で標準 PDS へ直接書き込む XRPC client。
 * OAuth 化するときは、createSession 部分を OAuth token provider に置き換えれば putRecord は残せる。
 */
class PdsDirectClient(
    private val mapper: WorkoutSessionRecordMapper = WorkoutSessionRecordMapper(),
) {
    private var cachedSession: Session? = null

    suspend fun write(history: CompanionWorkoutHistory, credentials: PdsCredentials): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val session = session(credentials)
                val ok = putRecord(history, credentials, session)
                if (ok) {
                    true
                } else {
                    cachedSession = null
                    putRecord(history, credentials, createSession(credentials))
                }
            }.onFailure { error ->
                Log.w(TAG, "PDS sync failed sourceRef=${history.id}: ${error.message}")
            }.getOrDefault(false)
        }

    private fun session(credentials: PdsCredentials): Session {
        val cached = cachedSession
        if (cached != null && cached.serviceUrl == credentials.serviceUrl && cached.identifier == credentials.identifier) {
            return cached
        }
        return createSession(credentials).also { cachedSession = it }
    }

    private fun createSession(credentials: PdsCredentials): Session {
        val response = xrpc(
            serviceUrl = credentials.serviceUrl,
            method = "POST",
            nsid = "com.atproto.server.createSession",
            body = buildJsonObject {
                put("identifier", credentials.identifier)
                put("password", credentials.appPassword)
            },
        )
        val did = response.string("did") ?: error("createSession response missing did")
        val accessJwt = response.string("accessJwt") ?: error("createSession response missing accessJwt")
        return Session(
            serviceUrl = credentials.serviceUrl,
            identifier = credentials.identifier,
            did = did,
            accessJwt = accessJwt,
        )
    }

    private fun putRecord(
        history: CompanionWorkoutHistory,
        credentials: PdsCredentials,
        session: Session,
    ): Boolean {
        val response = xrpc(
            serviceUrl = credentials.serviceUrl,
            method = "POST",
            nsid = "com.atproto.repo.putRecord",
            token = session.accessJwt,
            body = buildJsonObject {
                put("repo", session.did)
                put("collection", WorkoutSessionRecordMapper.COLLECTION)
                put("rkey", history.id)
                put("validate", false)
                put("record", mapper.map(history))
            },
            throwOnHttpError = false,
        )
        val status = response.string(HTTP_STATUS)?.toIntOrNull() ?: HTTP_OK
        if (status !in 200..299) {
            Log.w(TAG, "PDS putRecord rejected sourceRef=${history.id} status=$status")
            return false
        }
        return true
    }

    private fun xrpc(
        serviceUrl: String,
        method: String,
        nsid: String,
        token: String? = null,
        body: JsonObject? = null,
        throwOnHttpError: Boolean = true,
    ): JsonObject {
        val bytes = body?.toString()?.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL("${serviceUrl.trimEnd('/')}/xrpc/$nsid").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (bytes != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setFixedLengthStreamingMode(bytes.size)
            }
        }
        try {
            if (bytes != null) connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299 && throwOnHttpError) {
                error("$nsid -> HTTP $status $text")
            }
            val json = if (text.isBlank()) buildJsonObject { } else Json.parseToJsonElement(text).jsonObject
            return buildJsonObject {
                put(HTTP_STATUS, status.toString())
                json.forEach { (key, value) -> put(key, value) }
            }
        } finally {
            connection.disconnect()
        }
    }

    private data class Session(
        val serviceUrl: String,
        val identifier: String,
        val did: String,
        val accessJwt: String,
    )

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    companion object {
        private const val TAG = "PdsDirectClient"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val HTTP_STATUS = "_httpStatus"
        private const val HTTP_OK = 200
    }
}
