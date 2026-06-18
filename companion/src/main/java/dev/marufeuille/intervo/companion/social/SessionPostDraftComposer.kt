package dev.marufeuille.intervo.companion.social

import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import java.text.BreakIterator
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SessionPostDraft(
    val sourceRef: String,
    val posts: List<String>,
)

class SessionPostDraftComposer(
    private val maxGraphemes: Int = BLUESKY_POST_MAX_GRAPHEMES,
) {
    init {
        require(maxGraphemes >= 80) { "maxGraphemes must leave room for a title, one exercise, and context" }
    }

    fun compose(history: CompanionWorkoutHistory): SessionPostDraft {
        val title = history.titleForPost()
        val lines = parseExercises(history.exerciseSnapshotsJson, history.performedSetsJson)
            .sortedBy { it.order }
            .map { it.toPostLine() }

        return SessionPostDraft(
            sourceRef = history.id,
            posts = paginate(title = title, lines = lines),
        )
    }

    private fun paginate(title: String, lines: List<String>): List<String> {
        if (lines.isEmpty()) {
            return listOf(buildPost(header = "${title}を完了。", lines = emptyList(), includeHashTag = true))
        }

        val posts = mutableListOf<String>()
        var nextLineIndex = 0
        var page = 0
        while (nextLineIndex < lines.size) {
            val includeHashTag = page == 0
            val header = if (page == 0) "${title}を完了。" else "${title}を完了（続き）。"
            val pageLines = mutableListOf<String>()

            while (nextLineIndex < lines.size) {
                val candidateLines = pageLines + lines[nextLineIndex]
                val candidate = buildPost(header, candidateLines, includeHashTag)
                if (candidate.graphemeLength() <= maxGraphemes) {
                    pageLines += lines[nextLineIndex]
                    nextLineIndex += 1
                } else {
                    if (pageLines.isEmpty()) {
                        pageLines += fitSingleLine(header, lines[nextLineIndex], includeHashTag)
                        nextLineIndex += 1
                    }
                    break
                }
            }

            posts += buildPost(header, pageLines, includeHashTag)
            page += 1
        }

        return posts
    }

    private fun buildPost(header: String, lines: List<String>, includeHashTag: Boolean): String = buildString {
        append(header)
        if (lines.isNotEmpty()) {
            append("\n\n")
            append(lines.joinToString("\n"))
        }
        if (includeHashTag) {
            append("\n\n#Intervo")
        }
    }

    private fun fitSingleLine(header: String, line: String, includeHashTag: Boolean): String {
        var fitted = line
        while (
            buildPost(header, listOf(fitted), includeHashTag).graphemeLength() > maxGraphemes &&
            fitted.graphemeLength() > 4
        ) {
            fitted = fitted.takeGraphemes(fitted.graphemeLength() - 4) + "..."
        }
        return fitted
    }

    private fun parseExercises(
        snapshotsJson: String,
        performedSetsJson: String,
    ): List<ExerciseForPost> {
        val performedSetsByIndex = parsePerformedSets(performedSetsJson)
        val snapshots = snapshotsJson.parseArrayOrNull() ?: return emptyList()
        return snapshots.mapIndexedNotNull { index, element ->
            val snapshot = element as? JsonObject ?: return@mapIndexedNotNull null
            ExerciseForPost(
                name = snapshot.string("exercise_name")?.takeIf { it.isNotBlank() } ?: "種目${index + 1}",
                order = snapshot.int("sort_order")?.takeIf { it >= 0 } ?: index,
                mode = snapshot.string("mode").toMode(),
                plannedSets = snapshot.int("sets")?.takeIf { it >= 1 },
                plannedRepsPerSet = snapshot.int("reps_per_set").dropSentinel(),
                performedSets = performedSetsByIndex[index].orEmpty(),
            )
        }
    }

    private fun parsePerformedSets(performedSetsJson: String): Map<Int, List<PerformedSetForPost>> {
        val sets = performedSetsJson.parseArrayOrNull() ?: return emptyMap()
        return sets.mapIndexedNotNull { fallbackIndex, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val exerciseIndex = obj.int("exercise_index") ?: return@mapIndexedNotNull null
            exerciseIndex to PerformedSetForPost(
                index = obj.int("set_index") ?: fallbackIndex,
                reps = obj.int("reps")?.takeIf { it >= 0 },
                durationSeconds = obj.int("duration_seconds")?.takeIf { it >= 0 },
                completed = obj.boolean("completed") ?: true,
            )
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, setsForExercise) -> setsForExercise.sortedBy { it.index } }
    }

    private fun CompanionWorkoutHistory.titleForPost(): String {
        val snapshotTitle = workoutSnapshotJson.parseObjectOrNull()
            ?.string("workout_name")
            ?.takeIf { it.isNotBlank() }
        return workoutName.ifBlank { snapshotTitle ?: "トレーニング" }
    }

    private fun ExerciseForPost.toPostLine(): String {
        val completedSets = performedSets.count { it.completed }
        val hasEffort = performedSets.any { it.completed || (it.reps ?: 0) > 0 || (it.durationSeconds ?: 0) > 0 }
        if (completedSets == 0 && !hasEffort && plannedSets != null) {
            return "$name: 未実施（${plannedSets}セット予定）"
        }

        val setLabel = if (plannedSets != null) {
            "$completedSets/${plannedSets}セット"
        } else {
            "${completedSets}セット"
        }
        val details = buildList {
            val actualReps = performedSets.mapNotNull { it.reps }.sum()
            val hasReps = performedSets.any { it.reps != null }
            val plannedTotalReps = plannedSets?.let { sets -> plannedRepsPerSet?.let { reps -> sets * reps } }
            when {
                mode == ExerciseModeForPost.REPS && plannedTotalReps != null -> add("${actualReps}/${plannedTotalReps}回")
                hasReps && actualReps > 0 -> add("${actualReps}回")
            }

            val actualDurationSeconds = performedSets.mapNotNull { it.durationSeconds }.sum()
            if (mode == ExerciseModeForPost.TIME && actualDurationSeconds > 0) {
                add(actualDurationSeconds.formatDurationForPost())
            }
        }

        return buildString {
            append(name)
            append(": ")
            append(setLabel)
            if (details.isNotEmpty()) {
                append("・")
                append(details.joinToString("・"))
            }
        }
    }

    private fun String?.toMode(): ExerciseModeForPost? = when (this) {
        "TIMED", "time" -> ExerciseModeForPost.TIME
        "REPS", "reps" -> ExerciseModeForPost.REPS
        else -> null
    }

    private fun String.parseObjectOrNull(): JsonObject? =
        runCatching { Json.parseToJsonElement(this).jsonObject }.getOrNull()

    private fun String.parseArrayOrNull(): JsonArray? =
        runCatching { Json.parseToJsonElement(this).jsonArray }.getOrNull()

    private fun JsonObject.string(key: String): String? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

    private fun Int?.dropSentinel(): Int? = this?.takeIf { it >= 0 }

    private fun Int.formatDurationForPost(): String {
        if (this < 60) return "${this}秒"
        val minutes = this / 60
        val seconds = this % 60
        return if (seconds == 0) "${minutes}分" else "${minutes}分${seconds}秒"
    }

    private fun String.graphemeLength(): Int {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(this)
        var count = 0
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            boundary = iterator.next()
            if (boundary != BreakIterator.DONE) count += 1
        }
        return count
    }

    private fun String.takeGraphemes(count: Int): String {
        if (count <= 0) return ""
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(this)
        var boundary = iterator.first()
        var seen = 0
        var end = 0
        while (boundary != BreakIterator.DONE && seen < count) {
            boundary = iterator.next()
            if (boundary != BreakIterator.DONE) {
                seen += 1
                end = boundary
            }
        }
        return substring(0, end)
    }

    private data class ExerciseForPost(
        val name: String,
        val order: Int,
        val mode: ExerciseModeForPost?,
        val plannedSets: Int?,
        val plannedRepsPerSet: Int?,
        val performedSets: List<PerformedSetForPost>,
    )

    private data class PerformedSetForPost(
        val index: Int,
        val reps: Int?,
        val durationSeconds: Int?,
        val completed: Boolean,
    )

    private enum class ExerciseModeForPost { TIME, REPS }

    companion object {
        const val BLUESKY_POST_MAX_GRAPHEMES = 300
    }
}
