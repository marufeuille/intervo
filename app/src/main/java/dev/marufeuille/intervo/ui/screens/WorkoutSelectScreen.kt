package dev.marufeuille.intervo.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.Workout
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.data.WorkoutWithCount
import dev.marufeuille.intervo.sync.WorkoutHistorySyncClient
import dev.marufeuille.intervo.timer.TimerService
import dev.marufeuille.intervo.timer.TimerSnapshot
import dev.marufeuille.intervo.timer.TimerSnapshotStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import dev.marufeuille.intervo.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutSelectViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app), WorkoutHistorySyncClient(app))
    private val snapshotStore = TimerSnapshotStore(app)

    val workoutsWithCount: StateFlow<List<WorkoutWithCount>> = repo.workoutsWithCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingResume = MutableStateFlow<TimerSnapshot?>(null)
    val pendingResume: StateFlow<TimerSnapshot?> = _pendingResume.asStateFlow()

    // タイマーが実行中（オンゴーイングチップからの復帰など）なら、確認なしで直接タイマー画面へ戻す
    private val _runningWorkoutId = MutableStateFlow<String?>(null)
    val runningWorkoutId: StateFlow<String?> = _runningWorkoutId.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val running = TimerService.runningWorkoutId.value
            if (running != null) {
                _runningWorkoutId.value = running
                return@launch
            }
            val snapshot = snapshotStore.load() ?: return@launch
            if (System.currentTimeMillis() - snapshot.savedAtEpochMillis < SNAPSHOT_MAX_AGE_MILLIS) {
                _pendingResume.value = snapshot
            } else {
                snapshotStore.clear()
            }
        }
    }

    fun consumeResume() {
        _pendingResume.value = null
    }

    fun consumeRunningResume() {
        _runningWorkoutId.value = null
    }

    fun discardSnapshot() {
        val snapshot = _pendingResume.value ?: return
        _pendingResume.value = null
        viewModelScope.launch(Dispatchers.IO) {
            // 完了済みのぶんを部分履歴として残してから破棄する
            if (snapshot.state.elapsedSeconds > 0) {
                runCatching {
                    repo.addHistory(
                        workoutId = snapshot.workoutId,
                        workoutName = snapshot.workoutName,
                        totalSeconds = snapshot.state.elapsedSeconds,
                        exerciseCount = snapshot.state.exercises.size,
                        workoutSortOrder = snapshot.workoutSortOrder,
                        workoutExerciseType = snapshot.workoutExerciseType,
                        exercises = snapshot.state.exercises,
                        freeSetRecords = snapshot.state.freeSetRecords,
                    )
                }
            }
            snapshotStore.clear()
            // タイマーがバックグラウンドで生きている場合に備えて停止も指示する
            val context = getApplication<Application>()
            context.startService(
                Intent(context, TimerService::class.java).setAction(TimerService.ACTION_STOP)
            )
        }
    }

    fun deleteWorkout(wc: WorkoutWithCount) = viewModelScope.launch {
        repo.deleteWorkout(Workout(id = wc.id, name = wc.name, sortOrder = wc.sortOrder))
    }

    companion object {
        private const val SNAPSHOT_MAX_AGE_MILLIS = 12 * 60 * 60 * 1000L
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutSelectScreen(
    onWorkoutClick: (String) -> Unit,
    onAddWorkout: () -> Unit,
    onHistory: () -> Unit,
    onResumeWorkout: (String) -> Unit = {},
    vm: WorkoutSelectViewModel = viewModel()
) {
    val workoutsWithCount by vm.workoutsWithCount.collectAsStateWithLifecycle()
    val pendingResume by vm.pendingResume.collectAsStateWithLifecycle()
    val runningWorkoutId by vm.runningWorkoutId.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<WorkoutWithCount?>(null) }

    LaunchedEffect(runningWorkoutId) {
        runningWorkoutId?.let {
            vm.consumeRunningResume()
            onResumeWorkout(it)
        }
    }

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp)
        ) {
            item {
                Text(
                    text = "ワークアウト",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = TextSecondary, letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            if (workoutsWithCount.isEmpty()) {
                item {
                    Chip(
                        label = { Text("+ 追加して始めよう", color = ExerciseOrange, fontSize = 14.sp) },
                        onClick = onAddWorkout,
                        colors = ChipDefaults.chipColors(backgroundColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(workoutsWithCount.size) { i ->
                    val wc = workoutsWithCount[i]
                    WorkoutRow(
                        name = wc.name,
                        exerciseCount = wc.exerciseCount,
                        onClick = { onWorkoutClick(wc.id) },
                        onLongClick = { showDeleteDialog = wc }
                    )
                    if (i < workoutsWithCount.size - 1) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(DividerColor))
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Chip(
                        label = {
                            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                Text("＋  追加", color = ExerciseOrange, fontSize = 13.sp)
                            }
                        },
                        onClick = onAddWorkout,
                        colors = ChipDefaults.chipColors(backgroundColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Chip(
                    label = {
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text("履歴", color = TextSecondary, fontSize = 13.sp)
                        }
                    },
                    onClick = onHistory,
                    colors = ChipDefaults.chipColors(backgroundColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    showDeleteDialog?.let { target ->
        ConfirmDeleteDialog(
            name = target.name,
            onConfirm = { vm.deleteWorkout(target); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null }
        )
    }

    pendingResume?.let { snapshot ->
        ResumeWorkoutDialog(
            name = snapshot.workoutName,
            onResume = {
                vm.consumeResume()
                onResumeWorkout(snapshot.workoutId)
            },
            onDiscard = { vm.discardSnapshot() }
        )
    }
}

@Composable
private fun ResumeWorkoutDialog(name: String, onResume: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onDiscard) {
        Column(
            modifier = Modifier
                .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("前回のワークアウトが残っています。再開しますか？", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) { Text("破棄", fontSize = 11.sp, color = TextPrimary) }
                CompactButton(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange)
                ) { Text("再開", fontSize = 11.sp, color = Color.White) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutRow(
    name: String,
    exerciseCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("${exerciseCount}種目", fontSize = 11.sp, color = TextSecondary)
        }
        Text("›", fontSize = 18.sp, color = TextSecondary)
    }
}

@Composable
fun ConfirmDeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("削除しますか？", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) { Text("戻る", fontSize = 11.sp, color = TextPrimary) }
                CompactButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) { Text("削除", fontSize = 11.sp, color = Color.White) }
            }
        }
    }
}

@Composable
fun NameInputDialog(
    current: String,
    hint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xFF1C1C1C), shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(hint, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (text.isEmpty()) {
                    Text("入力してください", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f))
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) onConfirm(text.trim())
                    }),
                    cursorBrush = SolidColor(ExerciseOrange)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) { Text("✕", fontSize = 13.sp, color = TextPrimary) }
                CompactButton(
                    onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange)
                ) { Text("✓", fontSize = 13.sp, color = Color.White) }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
