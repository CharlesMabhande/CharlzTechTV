package com.charlztech.charlztechtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.charlztech.charlztechtv.ui.navigation.CharlzTechNavHost
import com.charlztech.charlztechtv.ui.theme.AppColors
import com.charlztech.charlztechtv.ui.theme.CharlzTechTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CharlzTechTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.Background
                ) {
                    CharlzTechNavHost()
                }
            }
        }
    }
}
