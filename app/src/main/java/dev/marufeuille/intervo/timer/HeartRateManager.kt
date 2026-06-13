package dev.marufeuille.intervo.timer

import android.content.Context
import android.util.Log
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ワークアウト中の心拍を Health Services の ExerciseClient で取得する。
 * 端末が心拍に対応していない場合や権限がない場合は静かに無効化され、heartRate は null のままになる。
 */
class HeartRateManager(context: Context) {

    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    private val _heartRate = MutableStateFlow<Int?>(null)
    /** 直近の心拍数（bpm）。未取得・非対応時は null。 */
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private var active = false

    private val callback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val samples = update.latestMetrics.getData(DataType.HEART_RATE_BPM)
            samples.lastOrNull()?.let {
                val bpm = it.value.toInt()
                Log.d(TAG, "hr sample: $bpm")
                _heartRate.value = bpm
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

        override fun onRegistered() {
            Log.i(TAG, "exercise update callback registered")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "exercise registration failed", throwable)
        }

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            Log.i(TAG, "availability changed: $dataType -> $availability")
            if (dataType == DataType.HEART_RATE_BPM &&
                availability is DataTypeAvailability &&
                availability != DataTypeAvailability.AVAILABLE
            ) {
                _heartRate.value = null
            }
        }
    }

    suspend fun start() {
        if (active) return
        val started = runCatching {
            val capabilities = exerciseClient.getCapabilitiesAsync().await()
            if (ExerciseType.WORKOUT !in capabilities.supportedExerciseTypes) {
                Log.w(TAG, "WORKOUT exercise type not supported")
                return
            }
            val exerciseCapabilities = capabilities.getExerciseTypeCapabilities(ExerciseType.WORKOUT)
            if (DataType.HEART_RATE_BPM !in exerciseCapabilities.supportedDataTypes) {
                Log.w(TAG, "HEART_RATE_BPM not supported for WORKOUT")
                return
            }

            exerciseClient.setUpdateCallback(callback)
            // センサー安定化のためウォームアップを試みる（非対応なら無視）
            runCatching {
                exerciseClient.prepareExerciseAsync(
                    WarmUpConfig(ExerciseType.WORKOUT, setOf(DataType.HEART_RATE_BPM))
                ).await()
            }
            val config = ExerciseConfig.builder(ExerciseType.WORKOUT)
                .setDataTypes(setOf(DataType.HEART_RATE_BPM))
                .setIsAutoPauseAndResumeEnabled(false)
                .setIsGpsEnabled(false)
                .build()
            exerciseClient.startExerciseAsync(config).await()
            Log.i(TAG, "exercise started")
        }.onFailure { Log.w(TAG, "failed to start exercise", it) }.isSuccess
        active = started
    }

    suspend fun stop() {
        if (!active) return
        active = false
        runCatching { exerciseClient.endExerciseAsync().await() }
        _heartRate.value = null
    }

    companion object {
        private const val TAG = "HeartRateManager"
    }
}
