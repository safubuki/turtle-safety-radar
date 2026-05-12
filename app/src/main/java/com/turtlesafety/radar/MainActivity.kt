package com.turtlesafety.radar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.ui.theme.TurtleSafetyRadarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TurtleSafetyRadarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScaffold()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Turtle Safety Radar") })
        }
    ) { padding ->
        Text(
            text = "Phase 0 scaffold — Parent Console は Phase 5 で実装します。",
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    TurtleSafetyRadarTheme {
        HomeScaffold()
    }
}
