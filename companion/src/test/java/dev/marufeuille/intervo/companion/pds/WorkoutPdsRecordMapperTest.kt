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

class WorkoutPdsRecordMapperTest {

    @Test
    fun mapsHistoryToWorkoutPlanRecord() {
        val mapper = mapper()

        val record = mapper.mapPlan(history())

        assertEquals(WorkoutPdsRecordMapper.PLAN_COLLECTION, record["\$type"]?.jsonPrimitive?.content)
        assertEquals("intervo", record["source"]?.jsonPrimitive?.content)
        assertEquals("workout-001", record["sourceRef"]?.jsonPrimitive?.content)
        assertEquals("strength_training", record["exerciseType"]?.jsonPrimitive?.content)
        assertEquals("上半身の日", record["title"]?.jsonPrimitive?.content)
        assertEquals("2026-06-17T12:20:40Z", record["createdAt"]?.jsonPrimitive?.content)
        assertFalse(record.containsKey("heartRate"))

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
        assertFalse(repsExercise.containsKey("performed"))

        val timedExercise = exercises[1].jsonObject
        assertEquals("time", timedExercise["mode"]?.jsonPrimitive?.content)
        val timedPlanned = timedExercise["planned"]?.jsonObject
        assertNotNull(timedPlanned)
        assertEquals(30, timedPlanned!!["durationSeconds"]?.jsonPrimitive?.int)
        assertEquals(2, timedPlanned["repRestSeconds"]?.jsonPrimitive?.int)
    }

    @Test
    fun mapsHistoryToWorkoutCheckinRecord() {
        val mapper = mapper()
        val planRef = PdsRecordRef(
            uri = "at://did:plc:alice/dev.marufeuille.workout.plan/workout-001",
            cid = "bafyplan",
        )

        val record = mapper.mapCheckin(history(), planRef)

        assertEquals(WorkoutPdsRecordMapper.CHECKIN_COLLECTION, record["\$type"]?.jsonPrimitive?.content)
        assertEquals("intervo", record["source"]?.jsonPrimitive?.content)
        assertEquals("session-001", record["sourceRef"]?.jsonPrimitive?.content)
        assertEquals("workout-001", record["planSourceRef"]?.jsonPrimitive?.content)
        assertEquals("上半身の日", record["title"]?.jsonPrimitive?.content)
        assertEquals("completed", record["status"]?.jsonPrimitive?.content)
        assertEquals("2026-06-17T12:00:00Z", record["startedAt"]?.jsonPrimitive?.content)
        assertEquals("2026-06-17T12:20:34Z", record["completedAt"]?.jsonPrimitive?.content)
        assertEquals(1234, record["durationSeconds"]?.jsonPrimitive?.int)
        assertEquals(2, record["exerciseCount"]?.jsonPrimitive?.int)
        assertEquals("2026-06-17T12:20:40Z", record["createdAt"]?.jsonPrimitive?.content)
        assertFalse(record.containsKey("heartRate"))
        assertFalse(record.containsKey("exercises"))

        val plan = record["plan"]?.jsonObject
        assertNotNull(plan)
        assertEquals(planRef.uri, plan!!["uri"]?.jsonPrimitive?.content)
        assertEquals(planRef.cid, plan["cid"]?.jsonPrimitive?.content)

        val performed = record["performed"]?.jsonObject
        assertNotNull(performed)
        assertEquals(3, performed!!["setCount"]?.jsonPrimitive?.int)
        assertEquals(2, performed["completedSetCount"]?.jsonPrimitive?.int)
    }

    @Test
    fun usesStableRkeys() {
        val mapper = mapper()
        val history = history()

        assertEquals("workout-001", mapper.planRkey(history))
        assertEquals("session-001", mapper.checkinRkey(history))
    }

    private fun mapper(): WorkoutPdsRecordMapper =
        WorkoutPdsRecordMapper(
            clock = Clock.fixed(Instant.parse("2026-06-17T12:20:40Z"), ZoneOffset.UTC),
        )

    private fun history(): CompanionWorkoutHistory {
        val completedAt = Instant.parse("2026-06-17T12:20:34Z")
        return CompanionWorkoutHistory(
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
        )
    }
}
