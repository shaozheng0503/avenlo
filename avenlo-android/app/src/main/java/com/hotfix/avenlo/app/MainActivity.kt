package com.hotfix.avenlo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hotfix.avenlo.app.ui.navigation.AvenloApp
import com.hotfix.avenlo.app.ui.theme.AvenloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(application)
        setContent {
            AvenloTheme {
                AvenloApp()
            }
        }
    }
}
