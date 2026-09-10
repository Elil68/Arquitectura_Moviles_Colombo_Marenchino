package com.example.tp1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tp1.ui.theme.TP1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("CicloDeVida", "MainActivity: onCreate")

        enableEdgeToEdge()

        setContent {
            TP1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaPrincipal(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("CicloDeVida", "MainActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CicloDeVida", "MainActivity: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CicloDeVida", "MainActivity: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CicloDeVida", "MainActivity: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CicloDeVida", "MainActivity: onDestroy")
    }
}

@Composable
fun PantallaPrincipal(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Â¡Hola Mundo!")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(context, SecondActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            Text(text = "Ir a la segunda pantalla")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaPrincipalPreview() {
    TP1Theme {
        PantallaPrincipal()
    }
}