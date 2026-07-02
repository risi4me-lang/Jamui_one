package com.example.jamuione

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.jamuione.ui.JamuiOneNavigation
import com.example.jamuione.ui.theme.JamuiOneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JamuiOneTheme {
                JamuiOneNavigation()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JamuiOnePreview() {
    JamuiOneTheme {
        JamuiOneNavigation()
    }
}
