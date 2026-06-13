package dev.marufeuille.intervo.timer

import dev.marufeuille.intervo.data.ExerciseHrInput

/** Health Connect の HeartRateRecord 用の、時刻つき心拍サンプル。 */
data class HrSample(val timeMillis: Long, val bpm: Int)

/**
 * ワークアウト中の心拍を集計する。全体の開始/平均/最大に加え、
 * 種目ごと（全セット通して）の開始時・終了時の心拍と、
 * Health Connect 連携用に間引いた時刻つきサンプル列を記録する。
 * Android 非依存の純粋クラス。
 */
class HrAccumulator {
    private var sum = 0L
    private var count = 0
    private var max = 0
    private var start: Int? = null
    private val perExercise = LinkedHashMap<Int, ExerciseHrAccum>()
    private val sampleSeries = ArrayList<HrSample>()
    private var lastSampleAtMillis = Long.MIN_VALUE

    /**
     * 心拍サンプルを1件記録する。exerciseIndex が null（運動外）なら種目別には集計しない。
     * timestampMillis を渡すと、SAMPLE_INTERVAL_MS 間隔・最大 MAX_SAMPLES 件まで
     * Health Connect 用のサンプル列にも間引いて追加する。
     */
    fun record(hr: Int, exerciseIndex: Int?, exerciseName: String, timestampMillis: Long = 0L) {
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
        if (timestampMillis > 0L &&
            sampleSeries.size < MAX_SAMPLES &&
            (lastSampleAtMillis == Long.MIN_VALUE ||
                timestampMillis - lastSampleAtMillis >= SAMPLE_INTERVAL_MS)
        ) {
            sampleSeries.add(HrSample(timestampMillis, hr))
            lastSampleAtMillis = timestampMillis
        }
    }

    fun samples(): List<HrSample> = sampleSeries.toList()

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
        sampleSeries.clear()
        lastSampleAtMillis = Long.MIN_VALUE
    }

    private class ExerciseHrAccum(
        val exerciseIndex: Int,
        val exerciseName: String,
        val startHr: Int,
    ) {
        var endHr: Int = startHr
    }

    companion object {
        const val SAMPLE_INTERVAL_MS = 5_000L
        const val MAX_SAMPLES = 1_000
    }
}
