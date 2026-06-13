package dev.marufeuille.intervo.data

object DefaultWorkouts {
    suspend fun insert(db: AppDatabase) {
        val upperBody = Workout(name = "上半身", sortOrder = 0)
        val lowerBody = Workout(name = "下半身", sortOrder = 1)
        db.workoutDao().insert(upperBody)
        db.workoutDao().insert(lowerBody)

        listOf(
            Exercise(workoutId = upperBody.id, name = "腕立て伏せ", durationSeconds = 30, sets = 3, restSeconds = 10, sortOrder = 0),
            Exercise(workoutId = upperBody.id, name = "プランク", durationSeconds = 30, sets = 3, restSeconds = 10, sortOrder = 1),
        ).forEach { db.exerciseDao().insert(it) }

        listOf(
            Exercise(workoutId = lowerBody.id, name = "スクワット", durationSeconds = 40, sets = 3, restSeconds = 20, sortOrder = 0),
            Exercise(workoutId = lowerBody.id, name = "ランジ", durationSeconds = 30, sets = 3, restSeconds = 15, sortOrder = 1),
        ).forEach { db.exerciseDao().insert(it) }
    }
}
