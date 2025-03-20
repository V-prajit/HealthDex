package com.example.phms

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

val biometricEnabledMap = mutableMapOf<String, Boolean>()

class MainActivity : FragmentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent {
            var isLoggedIn by remember { mutableStateOf(false) }
            var userToken by remember { mutableStateOf<String?>(null) }
            var firstName by remember { mutableStateOf<String?>(null) }
            val biometricAuth = BiometricAuth(this@MainActivity) { success ->
                if (success) {
                    isLoggedIn = true
                    firstName = auth.currentUser?.displayName
                }
            }
            if (isLoggedIn) {
                HomeScreen(firstName)
            } else {
                AuthScreen(auth, biometricAuth) { token, name ->
                    isLoggedIn = true
                    userToken = token
                    firstName = name
                }
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, biometricAuth: BiometricAuth, onLoginSuccess: (String, String?) -> Unit) {
    var isRegistering by remember { mutableStateOf(true) }
    var userToken by remember { mutableStateOf<String?>(null) }
    var showUserDetailsScreen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            showUserDetailsScreen -> {
                UserDetailsScreen(userToken) { token, firstName ->
                    onLoginSuccess(token, firstName)
                }
            }
            isRegistering -> {
                RegisterScreen(auth, onSwitch = { isRegistering = false }) { token ->
                    userToken = token
                    showUserDetailsScreen = true
                }
            }
            else -> {
                LoginScreen(auth, biometricAuth, onSwitch = { isRegistering = true }, onLoginSuccess)
            }
        }
    }
}

@Composable
fun RegisterScreen(auth: FirebaseAuth, onSwitch: () -> Unit, onRegistrationSuccess: (String) -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val enableBiometric = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))
        // Email Input
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email:") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        // Password Input
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = enableBiometric.value, onCheckedChange = { enableBiometric.value = it })
            Text("Enable Biometric")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.value.isBlank() || password.value.isBlank()) {
                    message.value = "Email and password cannot be empty"
                    return@Button
                }

                auth.createUserWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            if (firebaseUser != null) {
                                biometricEnabledMap[firebaseUser.uid] = enableBiometric.value
                                onRegistrationSuccess(firebaseUser.uid)
                            }
                        } else {
                            message.value = "Error: ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSwitch) {
            Text("Already have an account? Login here")
        }
    }
}

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    biometricAuth: BiometricAuth,
    onSwitch: () -> Unit,
    onLoginSuccess: (String, String?) -> Unit
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val firstName = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email:") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.value.isBlank() || password.value.isBlank()) {
                    message.value = "Email and password cannot be empty"
                    return@Button
                }

                auth.signInWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            if (firebaseUser != null) {
                                if (biometricEnabledMap[firebaseUser.uid] == true) {
                                    biometricAuth.authenticate()
                                }
                                fetchUserData(firebaseUser.uid) { user ->
                                    firstName.value = user?.firstName
                                    onLoginSuccess(firebaseUser.uid, user?.firstName)
                                }
                            }
                        } else {
                            message.value = "Login Failed: ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                biometricAuth.authenticateBiometric()
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Biometric Authentication")
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSwitch) {
            Text("Don't have an account? Register here")
        }
    }
}
