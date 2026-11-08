package com.auris.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auris.app.ui.AccuracyScreen
import com.auris.app.ui.AnalyseScreen
import com.auris.app.ui.AurisTheme
import com.auris.app.ui.ConditionsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AurisTheme {
                AurisApp()
            }
        }
    }
}

private data class Tab(val label: String, @DrawableRes val icon: Int)

private val tabs = listOf(
    Tab("Analyse", R.drawable.ic_analyse),
    Tab("Conditions", R.drawable.ic_conditions),
    Tab("Accuracy", R.drawable.ic_accuracy)
)

@Composable
fun AurisApp(vm: AurisViewModel = viewModel()) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selected) {
                0 -> AnalyseScreen(vm)
                1 -> ConditionsScreen(vm)
                else -> AccuracyScreen(vm)
            }
        }
    }
}
