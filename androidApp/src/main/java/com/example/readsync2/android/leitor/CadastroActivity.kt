package com.example.readsync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.example.readsync2.android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.userProfileChangeRequest

class CadastroActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id2))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            CadastroScreen(context = this, auth = auth, googleSignInClient = googleSignInClient)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException)
            {
                val statusCode = e.statusCode
                when (statusCode) {
                    // STATUS_CANCELED, STATUS_INTERRUPTED, etc.
                    else -> {
                        Log.e("GoogleSignIn", "Erro desconhecido: $e")
                    }
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, TelaPrincipalLeitor::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                }
            }
    }
}

const val RC_SIGN_IN = 9001

@Composable
fun CadastroScreen(
    context: CadastroActivity,
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient
) {
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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

        Text(
            text = "Cadastro",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuário") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage == "O campo Usuário não pode estar vazio."
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage == "O campo Email não pode estar vazio." || errorMessage == "Por favor, insira um email válido."
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage == "O campo Senha não pode estar vazio." || errorMessage == "A senha deve ter pelo menos 6 caracteres."
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val (isValid, message) = validateInputs(usuario, email, senha)
                if (isValid) {
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = usuario
                                }
                                user?.updateProfile(profileUpdates)
                                    ?.addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            val intent = Intent(context, TelaPrincipalLeitor::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            }
                                            startActivity(context, intent, null)
                                        } else {
                                            errorMessage = "Erro ao atualizar o perfil. Tente novamente."
                                        }
                                    }
                            } else {
                                errorMessage = "Erro ao criar a conta. Tente novamente."
                            }
                        }
                } else {
                    errorMessage = message
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Cadastrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = "Login com Google",
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    isLoading = true
                    val signInIntent = googleSignInClient.signInIntent
                    context.startActivityForResult(signInIntent, RC_SIGN_IN)
                }
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator()
        }
    }
}

fun validateInputs(
    usuario: String,
    email: String,
    senha: String
): Pair<Boolean, String?> {
    val emailRegex = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()

    return when {
        usuario.isBlank() -> Pair(false, "O campo Usuário não pode estar vazio.")
        email.isBlank() -> Pair(false, "O campo Email não pode estar vazio.")
        senha.isBlank() -> Pair(false, "O campo Senha não pode estar vazio.")
        senha.length < 6 -> Pair(false, "A senha deve ter pelo menos 6 caracteres.")
        !email.matches(emailRegex) -> Pair(false, "Por favor, insira um email válido.")
        else -> Pair(true, null)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewCadastroActivity() {
    CadastroScreen(context = CadastroActivity(), auth = FirebaseAuth.getInstance(), googleSignInClient = GoogleSignIn.getClient(CadastroActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN))
}
