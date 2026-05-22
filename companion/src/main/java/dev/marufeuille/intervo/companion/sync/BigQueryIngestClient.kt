package dev.marufeuille.intervo.companion.sync

import dev.marufeuille.intervo.companion.BuildConfig
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class BigQueryIngestClient {
    fun upload(
        endpoint: String,
        history: CompanionWorkoutHistory,
        authHeaders: AuthHeaders,
    ) {
        val body = JSONObject()
            .put("event_id", history.id)
            .put("source", "intervo_wear")
            .put("app_build_type", BuildConfig.BUILD_TYPE)
            .put("app_application_id", BuildConfig.APPLICATION_ID)
            .put("workout_id", history.workoutId)
            .put("workout_name", history.workoutName)
            .put("completed_at_millis", history.completedAt)
            .put("total_seconds", history.totalSeconds)
            .put("exercise_count", history.exerciseCount)
            .put("workout_snapshot", history.workoutSnapshotObject())
            .put("exercise_snapshots", history.exerciseSnapshotArray())
            .toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${authHeaders.firebaseIdToken}")
            setRequestProperty("X-Firebase-AppCheck", authHeaders.appCheckToken)
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: connection.responseMessage
                    ?: "HTTP $responseCode"
                throw IllegalStateException("Upload failed: HTTP $responseCode $message")
            }
        } finally {
            connection.disconnect()
        }
    }
}

private fun CompanionWorkoutHistory.workoutSnapshotObject(): JSONObject =
    if (workoutSnapshotJson.isBlank()) {
        JSONObject()
            .put("workout_id", workoutId)
            .put("workout_name", workoutName)
    } else {
        JSONObject(workoutSnapshotJson)
    }

private fun CompanionWorkoutHistory.exerciseSnapshotArray(): JSONArray =
    if (exerciseSnapshotsJson.isBlank()) JSONArray() else JSONArray(exerciseSnapshotsJson)
