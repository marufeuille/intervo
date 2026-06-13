package dev.marufeuille.intervo.tile

import dev.marufeuille.intervo.data.WorkoutWithCount

/** Tile に表示する 1 ワークアウト分の最小モデル */
data class TileWorkout(
    val id: String,
    val name: String,
    val exerciseCount: Int,
)

/** Tile に並べるワークアウト数の上限（丸画面に収まる範囲） */
const val TILE_MAX_WORKOUTS = 3

/**
 * Tile に出すワークアウトを決める純粋関数。一覧の先頭から最大 [maxItems] 件を取る。
 * レイアウト構築から切り離してユニットテストできるようにしている。
 */
fun tileWorkouts(all: List<WorkoutWithCount>, maxItems: Int = TILE_MAX_WORKOUTS): List<TileWorkout> =
    all.take(maxItems).map { TileWorkout(id = it.id, name = it.name, exerciseCount = it.exerciseCount) }
