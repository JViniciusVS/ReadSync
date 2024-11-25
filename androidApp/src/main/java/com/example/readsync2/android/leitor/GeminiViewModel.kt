package com.example.readsync2.android.leitor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GeminiViewModel : ViewModel() {
    private val _resposta = MutableStateFlow("")
    val resposta: StateFlow<String> = _resposta

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    var prompt by mutableStateOf("")

    fun callIA(value: String) {
        prompt = value
        viewModelScope.launch {
            GeminiIA()
        }
    }

    private suspend fun GeminiIA() {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "ADICIONE_SUA_API_KEY_AQUI"
            )
            val textoRetorno = generativeModel.generateContent(prompt).text.toString()
            _resposta.emit(textoRetorno)
        } catch (e: Exception) {
            _resposta.emit("Erro ao conectar com o Gemini: ${e.message}")
        }
    }
}
