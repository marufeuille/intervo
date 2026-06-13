package dev.marufeuille.intervo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.wear.ambient.AmbientLifecycleObserver
import dev.marufeuille.intervo.timer.TimerViewModel
import dev.marufeuille.intervo.ui.navigation.AppNavigation
import dev.marufeuille.intervo.ui.theme.IntervalTheme

class MainActivity : ComponentActivity() {

    private lateinit var timerViewModel: TimerViewModel
    private lateinit var ambientObserver: AmbientLifecycleObserver

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestTimerPermissions()

        timerViewModel = ViewModelProvider(this)[TimerViewModel::class.java]

        ambientObserver = AmbientLifecycleObserver(this, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                timerViewModel.setAmbient(true)
            }
            override fun onExitAmbient() {
                timerViewModel.setAmbient(false)
            }
            override fun onUpdateAmbient() {}
        })
        lifecycle.addObserver(ambientObserver)

        setContent {
            IntervalTheme {
                AppNavigation()
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
