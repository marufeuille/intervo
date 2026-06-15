package dev.marufeuille.intervo.companion.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import org.json.JSONArray
import org.json.JSONObject
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

        // Wear OS 上で運動中に実測した記録なので「手動入力」ではなく「アクティブ記録」として書き込む。
        // manualEntry のままだとカロミル等が手動エントリを運動時間の集計から除外し、時間が反映されない。
        val device = Device(type = Device.TYPE_WATCH)

        val samples = parseSamples(history.hrSamplesJson)
            .filter { !it.time.isBefore(start) && !it.time.isAfter(end) }
            .sortedBy { it.time }

        val records = mutableListOf<Record>()
        records += ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zone,
            endTime = end,
            endZoneOffset = zone,
            exerciseType = resolveExerciseType(history.workoutSnapshotJson),
            // title は付けない。title を設定すると Android Health の一覧で主見出しが
            // ワークアウト名に置き換わり、継続時間（start〜end）が前面に出なくなるため。
            // ワークアウト名は notes に入れ、一覧では「種別・◯分」を見せつつ詳細で名前を参照できるようにする。
            title = null,
            notes = history.workoutName.ifBlank { null },
            metadata = Metadata.activelyRecorded(device = device, clientRecordId = "session_${history.id}")
        )
        if (samples.isNotEmpty()) {
            records += HeartRateRecord(
                startTime = samples.first().time,
                startZoneOffset = zone,
                endTime = samples.last().time,
                endZoneOffset = zone,
                samples = samples.map { HeartRateRecord.Sample(it.time, it.bpm.toLong()) },
                metadata = Metadata.activelyRecorded(device = device, clientRecordId = "hr_${history.id}")
            )
        }

        return runCatching { client.insertRecords(records) }.isSuccess
    }

    /**
     * workout_snapshot_json 内の exercise_type（ExerciseCategory の enum 名）を
     * Health Connect の ExerciseSessionRecord.EXERCISE_TYPE_* 定数へ変換する。
     * 欠落・未知のキーは EXERCISE_TYPE_OTHER_WORKOUT にフォールバックする（既存データの後方互換）。
     */
    private fun resolveExerciseType(snapshotJson: String): Int = runCatching {
        if (snapshotJson.isBlank()) return@runCatching ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        when (JSONObject(snapshotJson).optString("exercise_type")) {
            "STRENGTH_TRAINING" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "HIGH_INTENSITY_INTERVAL_TRAINING" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "STRETCHING" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "CALISTHENICS" -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            "YOGA" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "RUNNING" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "WALKING" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }.getOrDefault(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT)

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
