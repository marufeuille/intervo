package dev.marufeuille.intervo.companion.pds

import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkoutSessionRecordMapperTest {

    @Test
    fun mapsHistoryToWorkoutSessionRecord() {
        val completedAt = Instant.parse("2026-06-17T12:20:34Z")
        val mapper = WorkoutSessionRecordMapper(
            clock = Clock.fixed(Instant.parse("2026-06-17T12:20:40Z"), ZoneOffset.UTC),
        )

        val record = mapper.map(
            CompanionWorkoutHistory(
                id = "session-001",
                workoutId = "workout-001",
                workoutName = "上半身の日",
                completedAt = completedAt.toEpochMilli(),
                totalSeconds = 1234,
                exerciseCount = 2,
                workoutSnapshotJson = """
                    {"workout_id":"workout-001","workout_name":"上半身の日","sort_order":0,"exercise_type":"STRENGTH_TRAINING"}
                """.trimIndent(),
                exerciseSnapshotsJson = """
                    [
                      {
                        "exercise_id":"bench",
                        "workout_id":"workout-001",
                        "exercise_name":"ベンチプレス",
                        "mode":"REPS",
                        "duration_seconds":-1,
                        "sets":3,
                        "rest_seconds":60,
                        "reps_per_set":10,
                        "rep_rest_seconds":0,
                        "sort_order":0
                      },
                      {
                        "exercise_id":"plank",
                        "workout_id":"workout-001",
                        "exercise_name":"プランク",
                        "mode":"TIMED",
                        "duration_seconds":30,
                        "sets":3,
                        "rest_seconds":30,
                        "reps_per_set":-1,
                        "rep_rest_seconds":2,
                        "sort_order":1
                      }
                    ]
                """.trimIndent(),
                startHr = 80,
                avgHr = 130,
                maxHr = 165,
                exerciseHrJson = """
                    [
                      {"exercise_index":0,"exercise_name":"ベンチプレス","start_hr":110,"end_hr":150,"sort_order":0},
                      {"exercise_index":1,"exercise_name":"プランク","start_hr":120,"end_hr":145,"sort_order":1}
                    ]
                """.trimIndent(),
                performedSetsJson = """
                    [
                      {"exercise_index":0,"exercise_name":"ベンチプレス","set_index":0,"reps":10,"completed":true,"sort_order":0},
                      {"exercise_index":0,"exercise_name":"ベンチプレス","set_index":1,"reps":9,"completed":false,"sort_order":1},
                      {"exercise_index":1,"exercise_name":"プランク","set_index":0,"duration_seconds":30,"completed":true,"sort_order":2}
                    ]
                """.trimIndent(),
            ),
        )

        assertEquals(WorkoutSessionRecordMapper.COLLECTION, record["\$type"]?.jsonPrimitive?.content)
        assertEquals("intervo", record["source"]?.jsonPrimitive?.content)
        assertEquals("session-001", record["sourceRef"]?.jsonPrimitive?.content)
        assertEquals("strength_training", record["exerciseType"]?.jsonPrimitive?.content)
        assertEquals("上半身の日", record["title"]?.jsonPrimitive?.content)
        assertEquals("2026-06-17T12:00:00Z", record["startedAt"]?.jsonPrimitive?.content)
        assertEquals("2026-06-17T12:20:34Z", record["completedAt"]?.jsonPrimitive?.content)
        assertEquals(1234, record["durationSeconds"]?.jsonPrimitive?.int)
        assertEquals("2026-06-17T12:20:40Z", record["createdAt"]?.jsonPrimitive?.content)

        val heartRate = record["heartRate"]?.jsonObject
        assertNotNull(heartRate)
        assertEquals(80, heartRate!!["start"]?.jsonPrimitive?.int)
        assertEquals(130, heartRate["avg"]?.jsonPrimitive?.int)
        assertEquals(165, heartRate["max"]?.jsonPrimitive?.int)

        val exercises = record["exercises"]?.jsonArray
        assertNotNull(exercises)
        assertEquals(2, exercises!!.size)

        val repsExercise = exercises[0].jsonObject
        assertEquals("ベンチプレス", repsExercise["name"]?.jsonPrimitive?.content)
        assertEquals("reps", repsExercise["mode"]?.jsonPrimitive?.content)
        assertEquals(0, repsExercise["order"]?.jsonPrimitive?.int)
        val repsPlanned = repsExercise["planned"]?.jsonObject
        assertNotNull(repsPlanned)
        assertEquals(3, repsPlanned!!["sets"]?.jsonPrimitive?.int)
        assertEquals(10, repsPlanned["reps"]?.jsonPrimitive?.int)
        assertEquals(60, repsPlanned["restSeconds"]?.jsonPrimitive?.int)
        assertFalse(repsPlanned.containsKey("durationSeconds"))
        val repsPerformed = repsExercise["performed"]?.jsonObject
        assertNotNull(repsPerformed)
        val repsPerformedSets = repsPerformed!!["sets"]?.jsonArray
        assertNotNull(repsPerformedSets)
        assertEquals(2, repsPerformedSets!!.size)
        assertEquals(0, repsPerformedSets[0].jsonObject["index"]?.jsonPrimitive?.int)
        assertEquals(10, repsPerformedSets[0].jsonObject["reps"]?.jsonPrimitive?.int)
        assertEquals("true", repsPerformedSets[0].jsonObject["completed"]?.jsonPrimitive?.content)
        assertEquals(1, repsPerformedSets[1].jsonObject["index"]?.jsonPrimitive?.int)
        assertEquals(9, repsPerformedSets[1].jsonObject["reps"]?.jsonPrimitive?.int)
        assertEquals("false", repsPerformedSets[1].jsonObject["completed"]?.jsonPrimitive?.content)
        val repsPerformedHr = repsPerformed["heartRate"]?.jsonObject
        assertNotNull(repsPerformedHr)
        assertEquals(110, repsPerformedHr!!["start"]?.jsonPrimitive?.int)
        assertEquals(150, repsPerformedHr["end"]?.jsonPrimitive?.int)

        val timedExercise = exercises[1].jsonObject
        assertEquals("time", timedExercise["mode"]?.jsonPrimitive?.content)
        val timedPlanned = timedExercise["planned"]?.jsonObject
        assertNotNull(timedPlanned)
        assertEquals(30, timedPlanned!!["durationSeconds"]?.jsonPrimitive?.int)
        assertFalse(timedPlanned.containsKey("reps"))
        assertEquals(2, timedPlanned["repRestSeconds"]?.jsonPrimitive?.int)
        val timedPerformedSets = timedExercise["performed"]?.jsonObject?.get("sets")?.jsonArray
        assertNotNull(timedPerformedSets)
        assertEquals(30, timedPerformedSets!![0].jsonObject["durationSeconds"]?.jsonPrimitive?.int)
    }
}
