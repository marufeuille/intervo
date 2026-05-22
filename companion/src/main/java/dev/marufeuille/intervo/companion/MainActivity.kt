package dev.marufeuille.intervo.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.marufeuille.intervo.companion.ui.CompanionApp
import dev.marufeuille.intervo.companion.ui.CompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionTheme {
                CompanionApp()
            }
        }
    }
}
