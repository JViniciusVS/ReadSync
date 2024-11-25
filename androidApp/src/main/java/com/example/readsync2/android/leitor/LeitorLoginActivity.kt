package com.example.readsync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.readsync2.android.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.auth.*

class LeitorLoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent {
            LoginScreen(context = this, auth = auth)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let {
                    firebaseAuthWithGoogle(it.idToken!!, auth, this)
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignInError", "Erro no login com o Google: ${e.statusCode}")
            }
        }
    }
}

@Composable
fun LoginScreen(context: LeitorLoginActivity, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val setErrorMessage: (String) -> Unit = { message -> errorMessage = message }

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
            text = "Login Leitor",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colors.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cadastre-se",
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .clickable {
                    val intent = Intent(context, CadastroActivity::class.java)
                    startActivity(context, intent, null)
                }
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = "Login com Google",
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    isLoading = true
                    googleSignIn(context)
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (validateInputs(email, senha)) {
                        isLoading = true
                        loginWithEmail(email, senha, auth, context, setErrorMessage) {
                            isLoading = false
                        }
                    } else {
                        setErrorMessage("Por favor, insira um e-mail e senha válidos!")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Entrar")
            }
        }
    }
}

fun validateInputs(email: String, senha: String): Boolean {
    val emailRegex = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()
    return email.matches(emailRegex) && senha.isNotEmpty()
}

fun loginWithEmail(
    email: String,
    senha: String,
    auth: FirebaseAuth,
    context: Context,
    setErrorMessage: (String) -> Unit,
    onComplete: () -> Unit
) {
    auth.signInWithEmailAndPassword(email, senha)
        .addOnCompleteListener { task ->
            onComplete()
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    val intent = Intent(context, TelaPrincipalLeitor::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(context, intent, null)
                }
            } else {
                val errorMessage = when {
                    task.exception?.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Credenciais inválidas. Verifique seu e-mail e senha."
                    else -> "Erro ao tentar fazer login: ${task.exception?.message}"
                }

                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()

                setErrorMessage(errorMessage)
                Log.e("LoginError", errorMessage)
            }
        }
}

fun googleSignIn(context: LeitorLoginActivity) {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id2))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val signInIntent = googleSignInClient.signInIntent
    context.startActivityForResult(signInIntent, RC_SIGN_IN)
}

fun firebaseAuthWithGoogle(idToken: String, auth: FirebaseAuth, context: Context) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    Log.d("GoogleSignIn", "Login bem-sucedido com o Google")
                    val intent = Intent(context, TelaPrincipalLeitor::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(context, intent, null)
                    (context as? Activity)?.finish()
                }
            } else {
                Log.e("GoogleSignInError", "Erro ao fazer login com o Google: ${task.exception?.message}")
            }
        }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewLeitorLoginActivity() {
    LoginScreen(context = LeitorLoginActivity(), auth = FirebaseAuth.getInstance())
}