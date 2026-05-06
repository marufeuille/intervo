package dev.marufeuille.intervo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.wear.ambient.AmbientLifecycleObserver
import dev.marufeuille.intervo.timer.TimerViewModel
import dev.marufeuille.intervo.ui.navigation.AppNavigation
import dev.marufeuille.intervo.ui.theme.IntervalTheme

class MainActivity : ComponentActivity() {

    private lateinit var timerViewModel: TimerViewModel
    private lateinit var ambientObserver: AmbientLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
}
