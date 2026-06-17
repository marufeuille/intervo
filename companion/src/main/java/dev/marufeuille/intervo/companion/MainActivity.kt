package dev.marufeuille.intervo.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.marufeuille.intervo.companion.ui.CompanionTheme
import dev.marufeuille.intervo.companion.ui.navigation.CompanionNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionTheme {
                CompanionNavHost()
            }
        }
    }
}
