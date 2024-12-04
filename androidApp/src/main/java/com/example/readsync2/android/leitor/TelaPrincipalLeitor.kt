package com.example.readsync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.readsync2.android.R
import com.google.firebase.firestore.FirebaseFirestore

class TelaPrincipalLeitor : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReaderHomeScreen(context = this)
        }
    }
}

@Composable
fun ReaderHomeScreen(context: TelaPrincipalLeitor) {
    var searchQuery by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    var livrosPorGenero by remember { mutableStateOf(mapOf<String, List<Livro>>()) }
    var livrosFiltradosPorGenero by remember { mutableStateOf(mapOf<String, List<Livro>>()) }

    // Buscar livros do Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("livros")
            .get()
            .addOnSuccessListener { result ->
                val groupedLivros = result.documents.mapNotNull { doc ->
                    val titulo = doc.getString("titulo")
                    val autor = doc.getString("autor")
                    val genero = doc.getString("genero")
                    val isFavorite = doc.getBoolean("isFavorite") ?: false
                    val id = doc.id
                    val urlCapa = doc.getString("urlCapa")
                    if (titulo != null && autor != null && genero != null) {
                        genero to Livro(id, titulo, autor, isFavorite, urlCapa)
                    } else {
                        null
                    }
                }.groupBy({ it.first }, { it.second })

                livrosPorGenero = groupedLivros
                livrosFiltradosPorGenero = groupedLivros // Inicialmente, exibe todos os livros
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao buscar livros: ${e.message}")
            }
    }

    // Atualiza os livros filtrados quando a query de pesquisa muda
    LaunchedEffect(searchQuery, showFavorites, livrosPorGenero) {
        livrosFiltradosPorGenero = livrosPorGenero.mapValues { (_, livros) ->
            livros.filter { livro ->
                (searchQuery.isBlank() || livro.titulo.contains(searchQuery, ignoreCase = true)) &&
                        (!showFavorites || livro.isFavorite)
            }
        }.filterValues { it.isNotEmpty() }
    }


    // Filtro de favoritos
    val livrosFiltrados = if (showFavorites) {
        livrosPorGenero.mapValues { entry ->
            entry.value.filter { it.isFavorite }
        }.filterValues { it.isNotEmpty() }
    } else {
        livrosPorGenero
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                val intent = Intent(context, TelaChatbot::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Ir para Chatbot")
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar livros") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = showFavorites,
                onCheckedChange = { showFavorites = it }
            )
            Text(text = "Favoritos", modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            livrosFiltradosPorGenero.forEach { (genero, livros) ->
                item {
                    Text(
                        text = genero,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                item {
                    LazyRow {
                        items(livros) { livro ->
                            LivroCard(
                                livro = livro,
                                onFavoriteClick = { updatedBook ->
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("livros")
                                        .document(updatedBook.id)
                                        .update("isFavorite", updatedBook.isFavorite)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "Livro atualizado com sucesso!")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Firestore", "Erro ao atualizar favorito: ${e.message}")
                                        }

                                    // Atualiza o estado local
                                    livrosPorGenero = livrosPorGenero.mapValues { (genero, livros) ->
                                        livros.map { livro ->
                                            if (livro.id == updatedBook.id) updatedBook else livro
                                        }
                                    }
                                },
                                onBookClick = {
                                    Log.d("LivroCard", "Livro clicado: ${livro.titulo}")
                                    val intent = Intent(context, LivroDetalhesActivity::class.java).apply {
                                        putExtra("livroId", livro.id)
                                        putExtra("admin", "false")
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivroCard(livro: Livro, onFavoriteClick: (Livro) -> Unit, onBookClick: (Livro) -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(300.dp)
            .clickable { onBookClick(livro) }
            .padding(8.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SubcomposeAsyncImage(
                model = livro.urlCapa,
                contentDescription = "Capa do livro",
                modifier = Modifier.size(120.dp),
                loading = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_book),
                        contentDescription = "Erro ao carregar imagem",
                        modifier = Modifier.size(120.dp)
                    )
                }
            )

            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = livro.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = livro.autor,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(
                    id = if (livro.isFavorite) R.drawable.ic_favorite_outline else R.drawable.ic_favorite_filled
                ),
                contentDescription = "Favoritar livro",
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        onFavoriteClick(livro.copy(isFavorite = !livro.isFavorite))
                    }
            )
        }
    }
}

data class Livro(val id: String, val titulo: String, val autor: String, var isFavorite: Boolean, val urlCapa: String?)

@Preview(showBackground = true)
@Composable
fun DefaultPreviewTelaPrincipalLeitor() {
    ReaderHomeScreen(context = TelaPrincipalLeitor())
}