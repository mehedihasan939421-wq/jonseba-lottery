package com.jonseba.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.lifecycle.lifecycleScope
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class Person(val uid: String, val name: String, val email: String, val status: String)

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminScreen(
            onLogin = { googleAdminLogin() },
            onLogout = { auth.signOut(); recreate() },
            isSignedIn = auth.currentUser != null,
            isAdmin = false
        ) }
    }

    private fun googleAdminLogin() {
        val clientId = getString(R.string.default_web_client_id)
        if (clientId.startsWith("PASTE_")) return
        val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        lifecycleScope.launch {
            try {
                val result = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity, request)
                val c = result.credential
                if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(c.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                        .addOnSuccessListener { recreate() }
                }
            } catch (_: Exception) { }
        }
    }
}

@Composable
private fun AdminScreen(onLogin: () -> Unit, onLogout: () -> Unit, isSignedIn: Boolean, isAdmin: Boolean) {
    var allowed by remember { mutableStateOf(false) }
    var people by remember { mutableStateOf(listOf<Person>()) }
    var message by remember { mutableStateOf("অ্যাডমিন অনুমতি যাচাই হচ্ছে") }
    val user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(user?.uid) {
        if (user != null) {
            user.getIdToken(true).addOnSuccessListener { token ->
                allowed = token.claims["admin"] == true
                message = if (allowed) "অ্যাডমিন ড্যাশবোর্ড" else "এই Google অ্যাকাউন্টে Admin অনুমতি নেই।"
                if (allowed) FirebaseFirestore.getInstance().collection("users")
                    .addSnapshotListener { snap, _ ->
                        people = snap?.documents?.map {
                            Person(it.id, it.getString("name") ?: "নাম নেই",
                                it.getString("email") ?: "", it.getString("status") ?: "active")
                        } ?: emptyList()
                    }
            }
        }
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("জনসেবা লটারি Admin", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            if (user == null) {
                Button(onClick = onLogin) { Text("Admin Google Login") }
            } else if (!allowed) {
                Text(message)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onLogout) { Text("লগআউট") }
            } else {
                Text(message)
                Button(onClick = onLogout) { Text("লগআউট") }
                Spacer(Modifier.height(12.dp))
                LazyColumn {
                    items(people) { person ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(person.name, style = MaterialTheme.typography.titleMedium)
                                Text(person.email)
                                Text("স্ট্যাটাস: ${person.status}")
                                Button(onClick = {
                                    val next = if (person.status == "blocked") "active" else "blocked"
                                    FirebaseFirestore.getInstance().collection("users").document(person.uid)
                                        .update("status", next)
                                }) { Text(if (person.status == "blocked") "Unblock" else "Block") }
                            }
                        }
                    }
                }
            }
        }
    }
}
