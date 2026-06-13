package dev.marufeuille.intervo.timer

import dev.marufeuille.intervo.data.ExerciseHrInput

/**
 * ワークアウト中の心拍を集計する。全体の開始/平均/最大に加え、
 * 種目ごと（全セット通して）の開始時・終了時の心拍を記録する。
 * Android 非依存の純粋クラス。
 */
class HrAccumulator {
    private var sum = 0L
    private var count = 0
    private var max = 0
    private var start: Int? = null
    private val perExercise = LinkedHashMap<Int, ExerciseHrAccum>()

    /** 心拍サンプルを1件記録する。exerciseIndex が null（運動外）なら種目別には集計しない。 */
    fun record(hr: Int, exerciseIndex: Int?, exerciseName: String) {
        if (hr <= 0) return
        if (start == null) start = hr
        sum += hr
        count++
        if (hr > max) max = hr
        if (exerciseIndex != null) {
            val acc = perExercise.getOrPut(exerciseIndex) {
                ExerciseHrAccum(exerciseIndex, exerciseName, hr)
            }
            acc.endHr = hr
        }
    }

    fun startHr(): Int? = start
    fun avgHr(): Int? = if (count > 0) (sum / count).toInt() else null
    fun maxHr(): Int? = if (count > 0) max else null

    fun exerciseRecords(): List<ExerciseHrInput> =
        perExercise.values
            .sortedBy { it.exerciseIndex }
            .mapIndexed { sortOrder, acc ->
                ExerciseHrInput(
                    exerciseIndex = acc.exerciseIndex,
                    exerciseName = acc.exerciseName,
                    startHr = acc.startHr,
                    endHr = acc.endHr,
                    sortOrder = sortOrder
                )
            }

    fun reset() {
        sum = 0L
        count = 0
        max = 0
        start = null
        perExercise.clear()
    }

    private class ExerciseHrAccum(
        val exerciseIndex: Int,
        val exerciseName: String,
        val startHr: Int,
    ) {
        var endHr: Int = startHr
    }
}
