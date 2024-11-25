package com.example.readsync

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LivroFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isEditMode = intent.getBooleanExtra("isEditMode", false)
        val livroId = intent.getStringExtra("livroId") // Recupera o ID do livro

        setContent {
            LivroFormScreen(isEditMode = isEditMode, livroId = livroId)
        }
    }
}

@Composable
fun LivroFormScreen(isEditMode: Boolean, livroId: String?) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf(TextFieldValue("")) }
    var autor by remember { mutableStateOf(TextFieldValue("")) }
    var paginas by remember { mutableStateOf(TextFieldValue("")) }
    var dataPublicacao by remember { mutableStateOf(TextFieldValue("")) }
    var urlCapa by remember { mutableStateOf(TextFieldValue("")) }
    var descricao by remember { mutableStateOf(TextFieldValue("")) }

    val generoOptions = listOf("Terror", "Comédia", "Drama", "Stand-Alone", "Aventura", "Ficção Científica", "Romance")
    var selectedGenero by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    // Carregar os dados do livro para edição
    if (isEditMode && livroId != null) {
        LaunchedEffect(livroId) {
            db.collection("livros").document(livroId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val livro = document.data
                        titulo = TextFieldValue(livro?.get("titulo") as String)
                        autor = TextFieldValue(livro?.get("autor") as String)
                        paginas = TextFieldValue((livro["paginas"] as Long).toString())
                        dataPublicacao = TextFieldValue(livro["dataPublicacao"] as String)
                        urlCapa = TextFieldValue(livro["urlCapa"] as String)
                        descricao = TextFieldValue(livro["descricao"] as String)
                        selectedGenero = livro["genero"] as String?
                    }
                }
        }
    }

    if (isSuccess) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Livro salvo com sucesso!", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(context, TelaPrincipalAdmin::class.java))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Botão de voltar
        Button(
            onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("Voltar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título
        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Autor
        OutlinedTextField(
            value = autor,
            onValueChange = { autor = it },
            label = { Text("Autor") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Páginas e Data de Publicação (mesma linha)
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = paginas,
                onValueChange = { paginas = it },
                label = { Text("Páginas") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.3f)
            )

            Spacer(modifier = Modifier.width(15.dp))

            OutlinedTextField(
                value = dataPublicacao,
                onValueChange = { dataPublicacao = it },
                label = { Text("Data de Publicação") },
                modifier = Modifier.weight(0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // URL da capa
        OutlinedTextField(
            value = urlCapa,
            onValueChange = { urlCapa = it },
            label = { Text("URL da Capa") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Exibir Chips de Gêneros
        Text(text = "Gêneros", fontSize = 18.sp)
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(generoOptions) { genero ->
                Chip(
                    selected = selectedGenero == genero,
                    onClick = {
                        selectedGenero = if (selectedGenero == genero) null else genero
                    }
                ) {
                    Text(text = genero)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Descrição
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botões
        Row(
            horizontalArrangement = if (isEditMode) Arrangement.SpaceBetween else Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isEditMode) {
                Button(onClick = {
                    // Ação para Deletar
                    if (livroId != null) {
                        db.collection("livros").document(livroId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Livro deletado com sucesso!", Toast.LENGTH_LONG).show()
                                context.startActivity(Intent(context, TelaPrincipalAdmin::class.java))
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Erro ao deletar livro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }) {
                    Text("Deletar")
                }
            }

            Button(onClick = {
                // Função para validar os campos
                fun areFieldsValid(): Boolean {
                    if (titulo.text.isEmpty() ||
                        autor.text.isEmpty() ||
                        paginas.text.isEmpty() ||
                        dataPublicacao.text.isEmpty() ||
                        urlCapa.text.isEmpty() ||
                        descricao.text.isEmpty() ||
                        selectedGenero == null
                    ) {
                        Toast.makeText(context, "Por favor, preencha todos os campos!", Toast.LENGTH_LONG).show()
                        return false
                    }
                    if (paginas.text.toIntOrNull() == null) {
                        Toast.makeText(context, "Insira um valor válido para o número de páginas!", Toast.LENGTH_LONG).show()
                        return false
                    }
                    return true
                }

                // Validar e salvar dados no Firebase
                if (areFieldsValid()) {
                    val livroData = hashMapOf(
                        "titulo" to titulo.text,
                        "autor" to autor.text,
                        "paginas" to paginas.text.toInt(),
                        "dataPublicacao" to dataPublicacao.text,
                        "urlCapa" to urlCapa.text,
                        "descricao" to descricao.text,
                        "genero" to selectedGenero,
                        "isFavorite" to false
                    )

                    // Se for edição, atualiza o livro
                    if (livroId != null) {
                        db.collection("livros").document(livroId)
                            .set(livroData)
                            .addOnSuccessListener {
                                isSuccess = true
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Erro ao salvar livro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        db.collection("livros")
                            .add(livroData)
                            .addOnSuccessListener {
                                isSuccess = true // Atualiza estado para redirecionar
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Erro ao salvar livro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }) {
                Text("Salvar")
            }
        }
    }
}

@Composable
fun Chip(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewLivroForm() {
    LivroFormScreen(isEditMode = false, livroId = null) // Modo de adicionar
}