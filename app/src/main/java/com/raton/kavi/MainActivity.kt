package com.raton.kavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.raton.kavi.ui.KaviApp
import com.raton.kavi.ui.theme.KaviTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaviTheme {
                KaviApp()
            }
        }
    }
}
