package dev.marufeuille.intervo.companion.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId

/**
 * 受信したワークアウト履歴を Health Connect に ExerciseSessionRecord + HeartRateRecord として書き込む。
 * clientRecordId を履歴 ID 由来にして、再試行時も重複登録されないようにする。
 */
class HealthConnectWriter(context: Context) {

    private val appContext = context.applicationContext

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(appContext) }

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable) return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.getOrDefault(false)
    }

    /** 書き込めたら true。非対応・権限なし・データ不正時は false。 */
    suspend fun write(history: CompanionWorkoutHistory): Boolean {
        if (!hasPermissions()) return false

        val end = Instant.ofEpochMilli(history.completedAt)
        val start = end.minusSeconds(history.totalSeconds.toLong().coerceAtLeast(1L))
        val zone = ZoneId.systemDefault().rules.getOffset(end)

        val samples = parseSamples(history.hrSamplesJson)
            .filter { !it.time.isBefore(start) && !it.time.isAfter(end) }
            .sortedBy { it.time }

        val records = mutableListOf<Record>()
        records += ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zone,
            endTime = end,
            endZoneOffset = zone,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            title = history.workoutName.ifBlank { null },
            metadata = Metadata.manualEntry(clientRecordId = "session_${history.id}")
        )
        if (samples.isNotEmpty()) {
            records += HeartRateRecord(
                startTime = samples.first().time,
                startZoneOffset = zone,
                endTime = samples.last().time,
                endZoneOffset = zone,
                samples = samples.map { HeartRateRecord.Sample(it.time, it.bpm.toLong()) },
                metadata = Metadata.manualEntry(clientRecordId = "hr_${history.id}")
            )
        }

        return runCatching { client.insertRecords(records) }.isSuccess
    }

    private data class ParsedSample(val time: Instant, val bpm: Long)

    private fun parseSamples(json: String): List<ParsedSample> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val t = obj.optLong("t", 0L)
            val bpm = obj.optInt("bpm", 0)
            if (t <= 0L || bpm <= 0) null else ParsedSample(Instant.ofEpochMilli(t), bpm.toLong())
        }
    }.getOrDefault(emptyList())

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
        )
    }
}
