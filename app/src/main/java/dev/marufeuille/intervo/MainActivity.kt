package dev.marufeuille.intervo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.wear.ambient.AmbientLifecycleObserver
import dev.marufeuille.intervo.ui.navigation.AppNavigation
import dev.marufeuille.intervo.ui.theme.IntervalTheme
import kotlinx.coroutines.flow.MutableStateFlow

/** Tile / Complication からワークアウト詳細を開くための Intent extra キー */
const val EXTRA_WORKOUT_ID = "dev.marufeuille.intervo.extra.WORKOUT_ID"

class MainActivity : ComponentActivity() {

    private lateinit var ambientObserver: AmbientLifecycleObserver

    /** Ambient（常時表示）状態の単一ソース。Ambient コールバックで更新し、Compose 側へ流す。 */
    private val ambient = MutableStateFlow(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestTimerPermissions()

        ambientObserver = AmbientLifecycleObserver(this, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                ambient.value = true
            }
            override fun onExitAmbient() {
                ambient.value = false
            }
            override fun onUpdateAmbient() {}
        })
        lifecycle.addObserver(ambientObserver)

        val initialWorkoutId = intent?.getStringExtra(EXTRA_WORKOUT_ID)

        setContent {
            IntervalTheme {
                AppNavigation(initialWorkoutId = initialWorkoutId, ambient = ambient)
            }
        }
    }

    // health タイプの Foreground Service（タイマー常駐）に必要な権限を起動時にまとめて要求する
    private fun requestTimerPermissions() {
        val needed = buildList {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            add(Manifest.permission.BODY_SENSORS)
            add(PERMISSION_READ_HEART_RATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private companion object {
        // SDK 定数が無いため文字列で指定（Wear OS 5+ の心拍読み取り権限）
        const val PERMISSION_READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
    }
}
