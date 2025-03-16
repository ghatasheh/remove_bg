package com.hisham.removebg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.removebg.ui.theme.RemoveBgTheme

class MainActivity : ComponentActivity() {
    val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoveBgTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    // load image from assets
                    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.bike)

                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()) {

                        Box(Modifier.fillMaxWidth()) {
                            Image(
                                bitmap = state.image ?: imageBitmap,
                                contentDescription = null,
                                modifier = Modifier.padding(innerPadding)
                            )

                            if (state.showLoading)
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }

                        Button(onClick = {
                            viewModel.removeBackground(imageBitmap)
                        }) {
                            Text(text = "Remove Background")
                        }
                    }
                }
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
    RemoveBgTheme {
        Greeting("Android")
    }
}