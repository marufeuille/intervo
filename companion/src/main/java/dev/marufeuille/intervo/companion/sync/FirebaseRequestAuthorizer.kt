package dev.marufeuille.intervo.companion.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class AuthHeaders(
    val firebaseIdToken: String,
    val appCheckToken: String,
    val uid: String,
)

class FirebaseRequestAuthorizer(context: Context) {
    init {
        val app = FirebaseApp.initializeApp(context.applicationContext)
        requireNotNull(app) {
            "Firebase is not configured. Add companion/src/debug/google-services.json or companion/src/release/google-services.json."
        }

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
    }

    suspend fun getHeaders(): AuthHeaders {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: auth.signInAnonymously().awaitTask().user
        requireNotNull(user) { "Firebase anonymous sign-in did not return a user." }

        val idToken = user.getIdToken(false).awaitTask().token
        require(!idToken.isNullOrBlank()) { "Firebase ID token is empty." }

        val appCheckToken = FirebaseAppCheck.getInstance().getToken(false).awaitTask().token
        require(appCheckToken.isNotBlank()) { "Firebase App Check token is empty." }

        return AuthHeaders(
            firebaseIdToken = idToken,
            appCheckToken = appCheckToken,
            uid = user.uid,
        )
    }

    val currentUid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }
}
