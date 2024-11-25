package com.example.readsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.readsync2.android.leitor.GeminiViewModel
import kotlinx.coroutines.flow.collectLatest

class TelaChatbot : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatbotScreen()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatbotScreen(viewModel: GeminiViewModel = viewModel()) {
    var userInput by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<String>()) }

    // Controlador do teclado
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observa o fluxo de resposta do GeminiViewModel
    LaunchedEffect(Unit) {
        viewModel.resposta.collectLatest { resposta ->
            if (resposta.isNotEmpty()) {
                chatHistory = chatHistory + "Chatbot: $resposta"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Exibe o histórico do chat
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(chatHistory) { message ->
                Text(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = if (message.startsWith("Chatbot:")) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row para incluir o botão dentro do campo de entrada
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Espaço entre os elementos
        ) {
            // Campo para entrada do usuário
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Digite sua mensagem") },
                modifier = Modifier.weight(1f) // Preenche o espaço disponível
            )

            // Botão de enviar mensagem
            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        // Adiciona a mensagem do usuário no histórico
                        chatHistory = chatHistory + "Você: $userInput"

                        // Chama o ViewModel para processar a mensagem
                        viewModel.callIA(userInput)

                        // Esconde o teclado após enviar a mensagem
                        keyboardController?.hide()

                        // Limpa o campo de entrada
                        userInput = ""
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically) // Alinha o botão ao centro verticalmente
            ) {
                Text("Enviar")
            }
        }
    }
}