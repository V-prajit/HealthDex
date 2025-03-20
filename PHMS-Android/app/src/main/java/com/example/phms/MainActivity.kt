package com.example.phms

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var biometricAuth: BiometricAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        biometricAuth=BiometricAuth(this){success->
            if (success){
                isLoggedIn= true
                firstName= auth.currentUser?.displayName
            }
        }
        setContent{
            var isLoggedIn by remember { mutableStateOf(false) }
            var userToken by remember { mutableStateOf<String?>(null) }
            var firstName by remember { mutableStateOf<String?>(null) }
            if (auth.currentUser!=null){
                biometricAuth.authenticate()
            }
            if (isLoggedIn) {
                HomeScreen(firstName)
            } else {
                AuthScreen(auth) { token, name ->
                    isLoggedIn = true
                    userToken = token
                    firstName = name
                }
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, onLoginSuccess: (String, String?) -> Unit){
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
                LoginScreen(auth, onSwitch = { isRegistering = true }, onLoginSuccess)
            }
        }
    }

}

@Composable
fun RegisterScreen(auth:FirebaseAuth, onSwitch: () -> Unit, onRegistrationSuccess: (String) -> Unit){
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }

    Column ( modifier = Modifier
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
            label = {Text("Email:")},
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Input
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = {Text("Password")},
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

                auth.createUserWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            if(firebaseUser != null){
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
fun LoginScreen(auth: FirebaseAuth, onSwitch: ()-> Unit, onLoginSuccess: (String, String?) -> Unit){
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
        Button(
            onClick = {
                biometricAuth.authenticate()
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ){
            Text("Login using Biometrics")
        }
        // Email Input
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = {Text("Email:")},
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Input
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = {Text("Password")},
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
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
                                fetchUserData(firebaseUser.uid){ user ->
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

        // Display message
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch to Register
        TextButton(onClick = onSwitch) {
            Text("Don't have an account? Register here")
        }
    }
}