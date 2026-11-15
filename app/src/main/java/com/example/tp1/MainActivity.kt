package com.example.tp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tp1.navigation.AppNavHost
import com.example.tp1.ui.tasks.LocalDemoScreen
import com.example.tp1.ui.theme.TP1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TP1Theme {
                if (BuildConfig.DEBUG && BuildConfig.LOCAL_DEMO) {
                    LocalDemoScreen()
                } else {
                    AppNavHost()
                }
            }
        }
    }
}
