package dev.marufeuille.intervo.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import dev.marufeuille.intervo.MainActivity
import dev.marufeuille.intervo.R
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.ExerciseCategory
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.sync.WorkoutHistorySyncClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private lateinit var vibrationManager: VibrationManager
    private lateinit var speechManager: SpeechManager
    private lateinit var beepManager: BeepManager
    private lateinit var repository: WorkoutRepository
    private lateinit var snapshotStore: TimerSnapshotStore
    private lateinit var heartRateManager: HeartRateManager
    private var heartRateJob: Job? = null
    private val hrAccumulator = HrAccumulator()
    private var lastPersistedKey: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var startedAtElapsedMillis: Long = 0L
    private var pausedAtElapsedMillis: Long = 0L
    private var totalPausedMillis: Long = 0L

    var activeWorkoutId: String? = null
        private set
    private var workoutName: String = ""
    private var workoutSortOrder: Int? = null
    private var workoutExerciseType: String = ExerciseCategory.DEFAULT.name
    private var historySaved = false

    override fun onCreate() {
        super.onCreate()
        vibrationManager = VibrationManager(this)
        speechManager = SpeechManager(this)
        beepManager = BeepManager()
        repository = WorkoutRepository(
            AppDatabase.getInstance(applicationContext),
            WorkoutHistorySyncClient(applicationContext)
        )
        snapshotStore = TimerSnapshotStore(applicationContext)
        heartRateManager = HeartRateManager(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stop()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_runningWorkoutId.value == activeWorkoutId) {
            _runningWorkoutId.value = null
        }
        stopHeartRate()
        releaseWakeLock()
        speechManager.shutdown()
        beepManager.release()
        serviceScope.cancel()
    }

    fun start(workoutId: String, resume: Boolean = false) {
        val current = _state.value
        val isRunning = current.phase !is TimerPhase.Idle && current.phase !is TimerPhase.Complete
        if (isRunning && workoutId == activeWorkoutId) return // 進行中の同じワークアウトには再接続するだけ
        serviceScope.launch {
            if (resume) {
                val snapshot = withContext(Dispatchers.IO) { snapshotStore.load() }
                if (snapshot != null && snapshot.workoutId == workoutId &&
                    snapshot.state.phase !is TimerPhase.Idle && snapshot.state.phase !is TimerPhase.Complete
                ) {
                    restoreFrom(snapshot)
                    return@launch
                }
            }
            val workout = repository.getWorkoutById(workoutId)
            val exercises = repository.getExercisesOnce(workoutId)
            val transition = TimerEngine.start(exercises) ?: return@launch
            activeWorkoutId = workoutId
            _runningWorkoutId.value = workoutId
            workoutName = workout?.name ?: ""
            workoutSortOrder = workout?.sortOrder
            workoutExerciseType = workout?.exerciseType ?: ExerciseCategory.DEFAULT.name
            historySaved = false
            lastPersistedKey = null
            promoteToForeground()
            acquireWakeLock()
            startHeartRate()
            startedAtElapsedMillis = SystemClock.elapsedRealtime()
            pausedAtElapsedMillis = 0L
            totalPausedMillis = 0L
            applyTransition(transition)
            startCountdown()
        }
    }

    private fun restoreFrom(snapshot: TimerSnapshot) {
        activeWorkoutId = snapshot.workoutId
        _runningWorkoutId.value = snapshot.workoutId
        workoutName = snapshot.workoutName
        workoutSortOrder = snapshot.workoutSortOrder
        workoutExerciseType = snapshot.workoutExerciseType
        historySaved = false
        lastPersistedKey = null
        promoteToForeground()
        acquireWakeLock()
        startHeartRate()
        startedAtElapsedMillis = SystemClock.elapsedRealtime() - snapshot.state.elapsedSeconds * 1000L
        pausedAtElapsedMillis = 0L
        totalPausedMillis = 0L
        _state.value = snapshot.state
        startCountdown()
    }

    private fun startHeartRate() {
        heartRateJob?.cancel()
        hrAccumulator.reset()
        heartRateJob = serviceScope.launch {
            heartRateManager.start()
            heartRateManager.heartRate.collect { hr ->
                if (hr != null) {
                    val phase = _state.value.phase
                    hrAccumulator.record(
                        hr = hr,
                        exerciseIndex = phase.exerciseIndexOrNull(),
                        exerciseName = phase.exerciseName(),
                        timestampMillis = System.currentTimeMillis()
                    )
                }
                _state.value = _state.value.copy(currentHeartRate = hr)
            }
        }
    }

    private fun TimerPhase.exerciseIndexOrNull(): Int? = when (this) {
        is TimerPhase.ExercisePhase -> exerciseIndex
        is TimerPhase.RepRestPhase -> exerciseIndex
        is TimerPhase.RestPhase -> exerciseIndex
        else -> null
    }

    private fun TimerPhase.exerciseName(): String =
        exerciseIndexOrNull()?.let { _state.value.exercises.getOrNull(it)?.name } ?: ""

    private fun stopHeartRate() {
        heartRateJob?.cancel()
        heartRateJob = null
        serviceScope.launch { heartRateManager.stop() }
    }

    fun pause() {
        if (_state.value.isPaused) return
        countdownJob?.cancel()
        _state.value = _state.value.copy(isPaused = true, elapsedSeconds = elapsedSecondsNow())
        pausedAtElapsedMillis = SystemClock.elapsedRealtime()
    }

    fun resume() {
        if (!_state.value.isPaused) return
        foldPauseTime()
        _state.value = _state.value.copy(isPaused = false, elapsedSeconds = elapsedSecondsNow())
        startCountdown()
    }

    fun skipRest() = applyUserAction { TimerEngine.skipRest(it) }

    fun adjustRest(deltaSeconds: Int) = applyUserAction { TimerEngine.adjustRest(it, deltaSeconds) }

    fun skipRep() = applyUserAction { TimerEngine.skipRep(it) }

    fun finishFreeSet(reps: Int? = null) = applyUserAction { TimerEngine.finishFreeSet(it, reps) }

    fun finishOpenEndedRepSet() = applyUserAction { TimerEngine.finishOpenEndedRepSet(it) }

    fun finishCurrentSet() = applyUserAction { TimerEngine.finishCurrentSet(it) }

    fun stop() {
        countdownJob?.cancel()
        stopHeartRate()
        releaseWakeLock()
        startedAtElapsedMillis = 0L
        pausedAtElapsedMillis = 0L
        totalPausedMillis = 0L
        activeWorkoutId = null
        _runningWorkoutId.value = null
        lastPersistedKey = null
        snapshotStore.clear()
        _state.value = TimerState()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun applyUserAction(action: (TimerState) -> TimerTransition?) {
        val transition = action(
            _state.value.copy(isPaused = false, elapsedSeconds = elapsedSecondsNow())
        ) ?: return
        countdownJob?.cancel()
        foldPauseTime()
        applyTransition(transition)
        restartCountdownIfActive()
    }

    private fun applyTransition(transition: TimerTransition) {
        _state.value = transition.state
        maybePersistSnapshot(transition.state)
        transition.effects.forEach { effect ->
            when (effect) {
                is TimerEffect.Vibrate -> vibrationManager.vibrate(effect.pattern)
                is TimerEffect.Speak -> speechManager.speak(effect.text)
                is TimerEffect.Beep -> beepManager.beep(effect.pattern)
                TimerEffect.WorkoutFinished -> {
                    releaseWakeLock()
                    finishWorkout(transition.state)
                }
            }
        }
    }

    // 毎秒の書き込みを避けるため、フェーズ境界（セット・レップ・休憩の切り替わり）でだけ保存する
    private fun maybePersistSnapshot(state: TimerState) {
        val key = when (val p = state.phase) {
            is TimerPhase.ExercisePhase -> "ex-${p.exerciseIndex}-${p.currentSet}-${p.currentRep}"
            is TimerPhase.RestPhase -> "rest-${p.exerciseIndex}-${p.completedSets}"
            is TimerPhase.RepRestPhase -> "represt-${p.exerciseIndex}-${p.currentSet}-${p.completedReps}"
            else -> null
        } ?: return
        if (key == lastPersistedKey) return
        lastPersistedKey = key
        val workoutId = activeWorkoutId ?: return
        val snapshot = TimerSnapshot(
            workoutId = workoutId,
            workoutName = workoutName,
            workoutSortOrder = workoutSortOrder,
            workoutExerciseType = workoutExerciseType,
            state = state,
            savedAtEpochMillis = System.currentTimeMillis()
        )
        serviceScope.launch(Dispatchers.IO) { snapshotStore.save(snapshot) }
    }

    private fun finishWorkout(finalState: TimerState) {
        val workoutId = activeWorkoutId
        _runningWorkoutId.value = null
        stopHeartRate()
        if (historySaved || workoutId == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        historySaved = true
        serviceScope.launch {
            runCatching {
                repository.addHistory(
                    workoutId = workoutId,
                    workoutName = workoutName,
                    totalSeconds = finalState.elapsedSeconds,
                    exerciseCount = finalState.exercises.size,
                    workoutSortOrder = workoutSortOrder,
                    workoutExerciseType = workoutExerciseType,
                    exercises = finalState.exercises,
                    freeSetRecords = finalState.freeSetRecords,
                    performedSetRecords = finalState.performedSetRecords,
                    startHr = hrAccumulator.startHr(),
                    avgHr = hrAccumulator.avgHr(),
                    maxHr = hrAccumulator.maxHr(),
                    exerciseHrRecords = hrAccumulator.exerciseRecords(),
                    hrSamples = hrAccumulator.samples(),
                )
            }
            withContext(Dispatchers.IO) { snapshotStore.clear() }
            ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun promoteToForeground() {
        runCatching {
            // startService で started 状態にしておくと、画面が unbind してもワークアウトが継続する
            startService(Intent(this, TimerService::class.java))
            NotificationManagerCompat.from(this).createNotificationChannel(
                NotificationChannelCompat.Builder(
                    getString(R.string.notification_channel_id),
                    NotificationManagerCompat.IMPORTANCE_LOW
                ).setName(getString(R.string.notification_channel_name)).build()
            )
            val touchIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_notification_timer)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_title))
                .setContentIntent(touchIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
                .setStaticIcon(R.drawable.ic_notification_timer)
                .setTouchIntent(touchIntent)
                .setStatus(
                    Status.Builder()
                        .addTemplate(getString(R.string.notification_title))
                        .build()
                )
                .build()
                .apply(applicationContext)
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        }
        // 通知権限や ACTIVITY_RECOGNITION が未許可だと SecurityException になるが、
        // その場合も従来どおりバインド + WakeLock のみで動作を継続する
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock?.release()
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "interval:timer").also {
            it.acquire(WAKE_LOCK_TIMEOUT_MILLIS)
        }
    }

    private fun foldPauseTime() {
        if (pausedAtElapsedMillis > 0L) {
            totalPausedMillis += SystemClock.elapsedRealtime() - pausedAtElapsedMillis
            pausedAtElapsedMillis = 0L
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            // delay(1000) の繰り返しでは処理時間ぶん遅れが累積するため、
            // elapsedRealtime 基準の目標時刻に合わせて次のティックを予約する
            var nextTickAt = SystemClock.elapsedRealtime() + 1000L
            while (true) {
                delay(nextTickAt - SystemClock.elapsedRealtime())
                nextTickAt += 1000L
                val current = _state.value
                if (current.isPaused) break
                if (current.phase is TimerPhase.Complete || current.phase is TimerPhase.Idle) break
                applyTransition(TimerEngine.tick(current.copy(elapsedSeconds = elapsedSecondsNow())))
            }
        }
    }

    private fun restartCountdownIfActive() {
        if (_state.value.phase !is TimerPhase.Complete) {
            startCountdown()
        }
    }

    private fun elapsedSecondsNow(): Int {
        if (startedAtElapsedMillis == 0L) return 0
        val now = SystemClock.elapsedRealtime()
        val currentPauseMillis = if (_state.value.isPaused && pausedAtElapsedMillis > 0L) {
            now - pausedAtElapsedMillis
        } else {
            0L
        }
        return ((now - startedAtElapsedMillis - totalPausedMillis - currentPauseMillis) / 1000L)
            .toInt()
            .coerceAtLeast(0)
    }

    companion object {
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 4 * 60 * 60 * 1000L
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "dev.marufeuille.intervo.action.STOP_TIMER"

        // ワークアウト実行中かをプロセス内で共有する。実行中にアプリを開き直した場合は
        // 確認ダイアログなしでタイマー画面へ直接復帰させるために使う
        private val _runningWorkoutId = MutableStateFlow<String?>(null)
        val runningWorkoutId: StateFlow<String?> = _runningWorkoutId.asStateFlow()
    }
}
