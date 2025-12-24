package com.mario.tanamin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import com.mario.tanamin.ui.route.TanamInAppRoute
import com.mario.tanamin.ui.theme.TanaminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: com.mario.tanamin.ui.viewmodel.AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val activeTheme = appViewModel.activeTheme.collectAsState().value

            TanaminTheme(activeTheme = activeTheme) {
                TanamInAppRoute(appViewModel = appViewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TanaminTheme {
        Greeting("Android")
    }
}