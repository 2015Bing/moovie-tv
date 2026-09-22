package com.moovie.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoovieApp() }
    }
}

@Composable
private fun MoovieApp(vm: MoovieViewModel = viewModel()) {
    MoovieTheme {
        MoovieRoot(vm)
    }
}
