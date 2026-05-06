package dev.marufeuille.intervo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.marufeuille.intervo.ui.screens.*

object Routes {
    const val WORKOUT_SELECT = "workout_select"
    const val WORKOUT_EDIT = "workout_edit?workoutId={workoutId}"
    const val WORKOUT_DETAIL = "workout_detail/{workoutId}"
    const val EXERCISE_EDIT = "exercise_edit/{workoutId}?exerciseId={exerciseId}"
    const val TIMER = "timer/{workoutId}"
    const val COMPLETION = "completion"

    fun workoutEdit(workoutId: String? = null) =
        if (workoutId != null) "workout_edit?workoutId=$workoutId" else "workout_edit"
    fun workoutDetail(workoutId: String) = "workout_detail/$workoutId"
    fun exerciseEdit(workoutId: String, exerciseId: String? = null) =
        if (exerciseId != null) "exercise_edit/$workoutId?exerciseId=$exerciseId"
        else "exercise_edit/$workoutId"
    fun timer(workoutId: String) = "timer/$workoutId"
}

@Composable
fun AppNavigation() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.WORKOUT_SELECT) {
        composable(Routes.WORKOUT_SELECT) {
            WorkoutSelectScreen(
                onWorkoutClick = { navController.navigate(Routes.workoutDetail(it)) },
                onAddWorkout = { navController.navigate(Routes.workoutEdit()) }
            )
        }
        composable(
            route = "workout_edit?workoutId={workoutId}",
            arguments = listOf(navArgument("workoutId") { nullable = true; defaultValue = null })
        ) { back ->
            WorkoutEditScreen(
                workoutId = back.arguments?.getString("workoutId"),
                onSaved = { workoutId ->
                    navController.navigate(Routes.workoutDetail(workoutId)) {
                        popUpTo(Routes.WORKOUT_SELECT)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "workout_detail/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { back ->
            val workoutId = back.arguments!!.getString("workoutId")!!
            WorkoutDetailScreen(
                workoutId = workoutId,
                onExerciseClick = { navController.navigate(Routes.exerciseEdit(workoutId, it)) },
                onAddExercise = { navController.navigate(Routes.exerciseEdit(workoutId)) },
                onStart = { navController.navigate(Routes.timer(workoutId)) }
            )
        }
        composable(
            route = "exercise_edit/{workoutId}?exerciseId={exerciseId}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType },
                navArgument("exerciseId") { nullable = true; defaultValue = null }
            )
        ) { back ->
            ExerciseEditScreen(
                workoutId = back.arguments!!.getString("workoutId")!!,
                exerciseId = back.arguments?.getString("exerciseId"),
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "timer/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { back ->
            TimerScreen(
                workoutId = back.arguments!!.getString("workoutId")!!,
                onComplete = {
                    navController.navigate(Routes.COMPLETION) {
                        popUpTo(Routes.WORKOUT_SELECT)
                    }
                },
                onStop = {
                    navController.navigate(Routes.WORKOUT_SELECT) {
                        popUpTo(Routes.WORKOUT_SELECT) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.COMPLETION) {
            CompletionScreen(
                onDone = {
                    navController.navigate(Routes.WORKOUT_SELECT) {
                        popUpTo(Routes.WORKOUT_SELECT) { inclusive = true }
                    }
                }
            )
        }
    }
}
