package com.gozdor.slatenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.SlateApp
import com.gozdor.slatenotes.ui.theme.SlateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel<AppHolder>().vm
            SlateTheme(vm.settings) {
                SlateApp(vm)
            }
        }
    }
}
