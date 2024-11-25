package com.example.readsync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelaInicial(context = this)
            Firebase.initialize(this)
        }
    }
}

@Composable
fun TelaInicial(context: MainActivity) {
    // Use verticalArrangement para centralizar verticalmente
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center, // Centraliza verticalmente
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Você é?",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp) // Espaço abaixo do texto
        )

        Button(
            onClick = {
                val intent = Intent(context, LeitorLoginActivity::class.java)
                intent.putExtra("tipoUsuario", "leitor")
                startActivity(context, intent, null)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Leitor")
        }

        Button(
            onClick = {
                val intent = Intent(context, AdminLoginActivity::class.java)
                intent.putExtra("tipoUsuario", "admin")
                startActivity(context, intent, null)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Administrador")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewMainActivity() {
    TelaInicial(context = MainActivity())
}