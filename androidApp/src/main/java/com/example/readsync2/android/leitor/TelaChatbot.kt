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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

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
fun ChatbotScreen() {
    var userInput by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<String>()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Digite sua mensagem") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        // Exibe a mensagem do usuário no histórico
                        chatHistory = chatHistory + "Você: $userInput"

                        // Faz a requisição para o endpoint
                        val client = OkHttpClient()
                        val json = JSONObject().put("pergunta", userInput).toString()
                        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)
                        val request = Request.Builder()
                            .url("https://api-gemini-1ck0.onrender.com/chat")
                            .post(requestBody)
                            .build()

                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("Chatbot", "Erro na requisição: $e")
                                chatHistory = chatHistory + "Erro: Não foi possível obter resposta."
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val respostaJson = response.body?.string()
                                val respostaTexto = JSONObject(respostaJson).getString("resposta")
                                chatHistory = chatHistory + "Chatbot: $respostaTexto"
                            }
                        })

                        userInput = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Enviar")
            }
        }
    }
}