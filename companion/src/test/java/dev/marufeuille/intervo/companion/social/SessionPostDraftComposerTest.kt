package dev.marufeuille.intervo.companion.social

import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPostDraftComposerTest {

    @Test
    fun composesPostWithEveryExerciseLine() {
        val draft = SessionPostDraftComposer().compose(sampleLowerBodyHistory())

        assertEquals("38e46db0-2b8c-42f2-b91c-fc2e8958dd5c", draft.sourceRef)
        assertEquals(
            listOf(
                """
                筋トレ下半身を完了。

                ブルガリアンスクワット右: 3/3セット・6回・2秒
                ブルガリアンスクワット左: 3/3セット・1回
                ヒップリフト: 未実施（3セット予定）

                #Intervo
                """.trimIndent(),
            ),
            draft.posts,
        )
    }

    @Test
    fun splitsLongPostWithoutDroppingExercises() {
        val history = sampleLowerBodyHistory(
            exerciseSnapshotsJson = buildExerciseSnapshotsJson(exerciseCount = 8),
            performedSetsJson = buildPerformedSetsJson(exerciseCount = 8),
        )

        val draft = SessionPostDraftComposer(maxGraphemes = 120).compose(history)
        val allText = draft.posts.joinToString("\n")

        assertTrue(draft.posts.size > 1)
        draft.posts.forEach { post -> assertTrue(post.length <= 120) }
        (1..8).forEach { index ->
            assertTrue(allText.contains("種目$index"))
        }
    }

    private fun sampleLowerBodyHistory(
        exerciseSnapshotsJson: String = """
            [
              {
                "exercise_name":"ブルガリアンスクワット右",
                "mode":"TIMED",
                "duration_seconds":-1,
                "sets":3,
                "rest_seconds":15,
                "reps_per_set":-1,
                "rep_rest_seconds":3,
                "sort_order":0
              },
              {
                "exercise_name":"ブルガリアンスクワット左",
                "mode":"TIMED",
                "duration_seconds":-1,
                "sets":3,
                "rest_seconds":15,
                "reps_per_set":-1,
                "rep_rest_seconds":3,
                "sort_order":1
              },
              {
                "exercise_name":"ヒップリフト",
                "mode":"REPS",
                "duration_seconds":-1,
                "sets":3,
                "rest_seconds":15,
                "reps_per_set":10,
                "rep_rest_seconds":2,
                "sort_order":2
              }
            ]
        """.trimIndent(),
        performedSetsJson: String = """
            [
              {"exercise_index":0,"exercise_name":"ブルガリアンスクワット右","set_index":0,"reps":3,"duration_seconds":0,"completed":true,"sort_order":0},
              {"exercise_index":0,"exercise_name":"ブルガリアンスクワット右","set_index":1,"reps":3,"duration_seconds":1,"completed":true,"sort_order":1},
              {"exercise_index":0,"exercise_name":"ブルガリアンスクワット右","set_index":2,"duration_seconds":1,"completed":true,"sort_order":2},
              {"exercise_index":1,"exercise_name":"ブルガリアンスクワット左","set_index":0,"reps":1,"duration_seconds":0,"completed":true,"sort_order":3},
              {"exercise_index":1,"exercise_name":"ブルガリアンスクワット左","set_index":1,"duration_seconds":0,"completed":true,"sort_order":4},
              {"exercise_index":1,"exercise_name":"ブルガリアンスクワット左","set_index":2,"duration_seconds":0,"completed":true,"sort_order":5},
              {"exercise_index":2,"exercise_name":"ヒップリフト","set_index":0,"reps":0,"duration_seconds":0,"completed":false,"sort_order":6},
              {"exercise_index":2,"exercise_name":"ヒップリフト","set_index":1,"reps":0,"duration_seconds":0,"completed":false,"sort_order":7},
              {"exercise_index":2,"exercise_name":"ヒップリフト","set_index":2,"reps":0,"duration_seconds":0,"completed":false,"sort_order":8}
            ]
        """.trimIndent(),
    ) = CompanionWorkoutHistory(
        id = "38e46db0-2b8c-42f2-b91c-fc2e8958dd5c",
        workoutId = "lower-body",
        workoutName = "筋トレ下半身",
        completedAt = Instant.parse("2026-06-17T21:38:07.263Z").toEpochMilli(),
        totalSeconds = 11,
        exerciseCount = 3,
        workoutSnapshotJson = """
            {"workout_id":"lower-body","workout_name":"筋トレ下半身","sort_order":0,"exercise_type":"STRENGTH_TRAINING"}
        """.trimIndent(),
        exerciseSnapshotsJson = exerciseSnapshotsJson,
        performedSetsJson = performedSetsJson,
    )

    private fun buildExerciseSnapshotsJson(exerciseCount: Int): String {
        val exercises = (1..exerciseCount).joinToString(",") { index ->
            """
            {
              "exercise_name":"種目$index",
              "mode":"REPS",
              "duration_seconds":-1,
              "sets":3,
              "rest_seconds":30,
              "reps_per_set":10,
              "rep_rest_seconds":0,
              "sort_order":${index - 1}
            }
            """.trimIndent()
        }
        return "[$exercises]"
    }

    private fun buildPerformedSetsJson(exerciseCount: Int): String {
        val sets = (0 until exerciseCount).joinToString(",") { exerciseIndex ->
            """
            {"exercise_index":$exerciseIndex,"set_index":0,"reps":10,"completed":true,"sort_order":$exerciseIndex}
            """.trimIndent()
        }
        return "[$sets]"
    }
}
