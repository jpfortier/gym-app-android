package dev.gymapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.gymapp.ui.theme.GymAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    val app = LocalContext.current.applicationContext as GymApplication
    var healthStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        healthStatus = withContext(Dispatchers.IO) {
            runCatching {
                val response = app.api.health()
                if (response.isSuccessful) {
                    response.body()?.status ?: "ok"
                } else {
                    "HTTP ${response.code()}"
                }
            }.getOrElse { "Error: ${it.message}" }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Gym App\nAPI: ${BuildConfig.BASE_URL}\nHealth: ${healthStatus ?: "..."}"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GymAppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Gym App\nAPI: ${BuildConfig.BASE_URL}\nHealth: ...")
        }
    }
}
