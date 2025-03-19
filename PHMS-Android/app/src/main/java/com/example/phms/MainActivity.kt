package com.example.phms

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.phms.ui.theme.PHMSTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.getEmptyLocaleList()
        )

        setContent {
            PHMSTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                var userToken by remember { mutableStateOf<String?>(null) }
                var firstName by remember { mutableStateOf<String?>(null) }
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingScreen(onBackClick = { showSettings = false })
                } else if (isLoggedIn) {
                    HomeScreen(
                        firstName = firstName ?: "No_Name",
                        onSettingsClick = { showSettings = true }
                    )
                } else {
                    AuthScreen(
                        auth = auth,
                        onLoginSuccess =  { token, name ->
                            isLoggedIn = true
                            userToken = token
                            firstName = name
                        },
                        onSettingsClick = { showSettings = true }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, onLoginSuccess: (String, String?) -> Unit, onSettingsClick: () -> Unit){
    var isRegistering by remember { mutableStateOf(true) }
    var userToken by remember { mutableStateOf<String?>(null) }
    var showUserDetailsScreen by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
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
        Text(text = stringResource(R.string.register), style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))
        // Email Input
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = {Text(stringResource(R.string.email))},
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Input
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = {Text(stringResource(R.string.password))},
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
            Text(stringResource(R.string.register))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitch) {
            Text(stringResource( R.string.already_have_account))
        }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth, onSwitch: ()-> Unit, onLoginSuccess: (String, String?) -> Unit){
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val firstName = remember { mutableStateOf<String?>(null) }
    val errorEmptyFieldsText = stringResource(R.string.error_empty_fields)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.login), style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Email Input
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = {Text(stringResource(R.string.email))},
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Input
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = {Text(stringResource(R.string.password))},
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        val loginFailedTemplate = stringResource(R.string.login_failed)
        // Login Button
        Button(
            onClick = {
                if (email.value.isBlank() || password.value.isBlank()) {
                    message.value =  errorEmptyFieldsText
                    return@Button
                }

                auth.signInWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            if (firebaseUser != null) {
                                Log.d("LoginScreen", "Login successful, fetching user data for ${firebaseUser.uid}")
                                fetchUserData(firebaseUser.uid){ user ->
                                    Log.d("LoginScreen", "User data fetched: ${user?.firstName}")
                                    firstName.value = user?.firstName
                                    onLoginSuccess(firebaseUser.uid, user?.firstName)
                                }
                            }
                        } else {
                            message.value = String.format(loginFailedTemplate, task.exception?.message ?: "")
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(stringResource(R.string.login_button))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display message
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch to Register
        TextButton(onClick = onSwitch) {
            Text(stringResource(R.string.dont_have_account))
        }
    }
}