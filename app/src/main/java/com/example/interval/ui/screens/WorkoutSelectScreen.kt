package dev.marufeuille.intervo.ui.screens

import android.app.Application
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
import kotlinx.coroutines.flow.first
import dev.marufeuille.intervo.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutSelectViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app))

    val workoutsWithCount: StateFlow<List<WorkoutWithCount>> = repo.workoutsWithCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteWorkout(wc: WorkoutWithCount) = viewModelScope.launch {
        repo.deleteWorkout(Workout(id = wc.id, name = wc.name, sortOrder = wc.sortOrder))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutSelectScreen(
    onWorkoutClick: (String) -> Unit,
    onAddWorkout: () -> Unit,
    vm: WorkoutSelectViewModel = viewModel()
) {
    val workoutsWithCount by vm.workoutsWithCount.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<WorkoutWithCount?>(null) }

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
        }
    }

    showDeleteDialog?.let { target ->
        ConfirmDeleteDialog(
            name = target.name,
            onConfirm = { vm.deleteWorkout(target); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null }
        )
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
