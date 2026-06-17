package dev.marufeuille.intervo.timer

import android.content.Context
import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.ExerciseCategory
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.FreeSetRecordInput
import dev.marufeuille.intervo.data.PerformedSetRecordInput
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 実行中ワークアウトの進行状態。プロセスキルやクラッシュ後に
 * 再開・部分履歴保存できるよう、フェーズ境界ごとにファイルへ永続化する。
 */
data class TimerSnapshot(
    val workoutId: String,
    val workoutName: String,
    val workoutSortOrder: Int?,
    val workoutExerciseType: String = ExerciseCategory.DEFAULT.name,
    val state: TimerState,
    val savedAtEpochMillis: Long
)

class TimerSnapshotStore(context: Context) {

    private val file = File(context.filesDir, "timer_snapshot.json")

    fun save(snapshot: TimerSnapshot) {
        runCatching { file.writeText(toJson(snapshot).toString()) }
    }

    fun load(): TimerSnapshot? = runCatching {
        if (!file.exists()) return null
        fromJson(JSONObject(file.readText()))
    }.getOrNull()

    fun clear() {
        runCatching { file.delete() }
    }

    private fun toJson(snapshot: TimerSnapshot): JSONObject = JSONObject().apply {
        put("workoutId", snapshot.workoutId)
        put("workoutName", snapshot.workoutName)
        snapshot.workoutSortOrder?.let { put("workoutSortOrder", it) }
        put("workoutExerciseType", snapshot.workoutExerciseType)
        put("savedAtEpochMillis", snapshot.savedAtEpochMillis)
        put("elapsedSeconds", snapshot.state.elapsedSeconds)
        put("phase", phaseToJson(snapshot.state.phase))
        put("exercises", JSONArray().apply {
            snapshot.state.exercises.forEach { put(exerciseToJson(it)) }
        })
        put("freeSetRecords", JSONArray().apply {
            snapshot.state.freeSetRecords.forEach { put(freeSetRecordToJson(it)) }
        })
        put("performedSetRecords", JSONArray().apply {
            snapshot.state.performedSetRecords.forEach { put(performedSetRecordToJson(it)) }
        })
    }

    private fun fromJson(json: JSONObject): TimerSnapshot {
        val exercises = json.getJSONArray("exercises").let { array ->
            (0 until array.length()).map { exerciseFromJson(array.getJSONObject(it)) }
        }
        val freeSetRecords = json.getJSONArray("freeSetRecords").let { array ->
            (0 until array.length()).map { freeSetRecordFromJson(array.getJSONObject(it)) }
        }
        val performedSetRecords = json.optJSONArray("performedSetRecords")?.let { array ->
            (0 until array.length()).map { performedSetRecordFromJson(array.getJSONObject(it)) }
        }.orEmpty()
        return TimerSnapshot(
            workoutId = json.getString("workoutId"),
            workoutName = json.getString("workoutName"),
            workoutSortOrder = if (json.has("workoutSortOrder")) json.getInt("workoutSortOrder") else null,
            workoutExerciseType = if (json.has("workoutExerciseType")) json.getString("workoutExerciseType") else ExerciseCategory.DEFAULT.name,
            state = TimerState(
                exercises = exercises,
                phase = phaseFromJson(json.getJSONObject("phase")),
                isPaused = false,
                elapsedSeconds = json.getInt("elapsedSeconds"),
                freeSetRecords = freeSetRecords,
                performedSetRecords = performedSetRecords,
            ),
            savedAtEpochMillis = json.getLong("savedAtEpochMillis")
        )
    }

    private fun phaseToJson(phase: TimerPhase): JSONObject = JSONObject().apply {
        when (phase) {
            is TimerPhase.ExercisePhase -> {
                put("type", "exercise")
                put("exerciseIndex", phase.exerciseIndex)
                put("currentSet", phase.currentSet)
                put("currentRep", phase.currentRep)
                put("remainingSeconds", phase.remainingSeconds)
            }
            is TimerPhase.RepRestPhase -> {
                put("type", "repRest")
                put("exerciseIndex", phase.exerciseIndex)
                put("currentSet", phase.currentSet)
                put("completedReps", phase.completedReps)
                put("remainingSeconds", phase.remainingSeconds)
            }
            is TimerPhase.RestPhase -> {
                put("type", "rest")
                put("exerciseIndex", phase.exerciseIndex)
                put("completedSets", phase.completedSets)
                put("remainingSeconds", phase.remainingSeconds)
            }
            TimerPhase.Complete -> put("type", "complete")
            TimerPhase.Idle -> put("type", "idle")
        }
    }

    private fun phaseFromJson(json: JSONObject): TimerPhase = when (json.getString("type")) {
        "exercise" -> TimerPhase.ExercisePhase(
            exerciseIndex = json.getInt("exerciseIndex"),
            currentSet = json.getInt("currentSet"),
            currentRep = json.getInt("currentRep"),
            remainingSeconds = json.getInt("remainingSeconds")
        )
        "repRest" -> TimerPhase.RepRestPhase(
            exerciseIndex = json.getInt("exerciseIndex"),
            currentSet = json.getInt("currentSet"),
            completedReps = json.getInt("completedReps"),
            remainingSeconds = json.getInt("remainingSeconds")
        )
        "rest" -> TimerPhase.RestPhase(
            exerciseIndex = json.getInt("exerciseIndex"),
            completedSets = json.getInt("completedSets"),
            remainingSeconds = json.getInt("remainingSeconds")
        )
        "complete" -> TimerPhase.Complete
        else -> TimerPhase.Idle
    }

    private fun exerciseToJson(exercise: Exercise): JSONObject = JSONObject().apply {
        put("id", exercise.id)
        put("workoutId", exercise.workoutId)
        put("name", exercise.name)
        put("mode", exercise.mode.name)
        put("durationSeconds", exercise.durationSeconds)
        put("sets", exercise.sets)
        put("restSeconds", exercise.restSeconds)
        put("repsPerSet", exercise.repsPerSet)
        put("repRestSeconds", exercise.repRestSeconds)
        put("sortOrder", exercise.sortOrder)
    }

    private fun exerciseFromJson(json: JSONObject): Exercise = Exercise(
        id = json.getString("id"),
        workoutId = json.getString("workoutId"),
        name = json.getString("name"),
        mode = ExerciseMode.valueOf(json.getString("mode")),
        durationSeconds = json.getInt("durationSeconds"),
        sets = json.getInt("sets"),
        restSeconds = json.getInt("restSeconds"),
        repsPerSet = json.getInt("repsPerSet"),
        repRestSeconds = json.getInt("repRestSeconds"),
        sortOrder = json.getInt("sortOrder")
    )

    private fun freeSetRecordToJson(record: FreeSetRecordInput): JSONObject = JSONObject().apply {
        put("exerciseId", record.exerciseId)
        put("exerciseName", record.exerciseName)
        put("setNumber", record.setNumber)
        put("durationSeconds", record.durationSeconds)
        record.reps?.let { put("reps", it) }
        put("sortOrder", record.sortOrder)
    }

    private fun freeSetRecordFromJson(json: JSONObject): FreeSetRecordInput = FreeSetRecordInput(
        exerciseId = json.getString("exerciseId"),
        exerciseName = json.getString("exerciseName"),
        setNumber = json.getInt("setNumber"),
        durationSeconds = json.getInt("durationSeconds"),
        reps = if (json.has("reps")) json.getInt("reps") else null,
        sortOrder = json.getInt("sortOrder")
    )

    private fun performedSetRecordToJson(record: PerformedSetRecordInput): JSONObject = JSONObject().apply {
        put("exerciseIndex", record.exerciseIndex)
        put("exerciseName", record.exerciseName)
        put("setIndex", record.setIndex)
        record.durationSeconds?.let { put("durationSeconds", it) }
        record.reps?.let { put("reps", it) }
        put("completed", record.completed)
        put("sortOrder", record.sortOrder)
    }

    private fun performedSetRecordFromJson(json: JSONObject): PerformedSetRecordInput = PerformedSetRecordInput(
        exerciseIndex = json.getInt("exerciseIndex"),
        exerciseName = json.getString("exerciseName"),
        setIndex = json.getInt("setIndex"),
        durationSeconds = if (json.has("durationSeconds")) json.getInt("durationSeconds") else null,
        reps = if (json.has("reps")) json.getInt("reps") else null,
        completed = json.optBoolean("completed", true),
        sortOrder = json.getInt("sortOrder")
    )
}
