package com.example.phms

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.Composable as Composable1

val biometricEnabledMap = mutableMapOf<String, Boolean>()

class MainActivity : FragmentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!appPrefs.contains("has_launched_before")) {
            appPrefs.edit().putBoolean("has_launched_before", true).apply()
            auth.signOut()
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.getEmptyLocaleList()
        )

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
            var darkModeEnabled by remember { mutableStateOf(prefs.getBoolean("DARK_MODE", false)) }
            PHMSTheme (darkTheme = darkModeEnabled) {
                var isLoggedIn by remember { mutableStateOf(false) }
                var userToken by remember { mutableStateOf<String?>(null) }
                var firstName by remember { mutableStateOf<String?>(null) }
                var showSettings by remember { mutableStateOf(false) }

                val biometricAuth = BiometricAuth(this@MainActivity) { success, name ->
                    if (success) {
                        isLoggedIn = true
                        firstName = name
                    }
                }

                LaunchedEffect(Unit) {
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val biometricEnabled = prefs.getBoolean("LAST_USER_BIOMETRIC", false)
                    if (!biometricEnabled) {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            userToken = currentUser.uid
                            fetchUserData(currentUser.uid) { userData ->
                                firstName = userData?.firstName
                                isLoggedIn = true
                            }
                        }
                    }
                }
                Log.d("MainActivity", "Rendering: isLoggedIn=$isLoggedIn, showSettings=$showSettings")
                when {
                    showSettings -> {
                        Log.d("MainActivity", "Showing Settings Screen")
                        SettingScreen(
                            onBackClick = {
                                Log.d("MainActivity", "Settings Back clicked")
                                showSettings = false
                            },
                            onLogout = {
                                Log.d("MainActivity", "Logout clicked")
                                auth.signOut()
                                isLoggedIn = false
                                userToken = null
                                firstName = null
                                showSettings = false
                            }
                        )
                    }
                    isLoggedIn -> {
                        Log.d("MainActivity", "Showing Dashboard Screen")
                        DashboardScreen(
                            firstName = firstName,
                            userToken = userToken,
                            onSettingsClick = {
                                Log.d("MainActivity", "Settings button clicked")
                                showSettings = true
                            }
                        )
                    }
                    else -> {
                        Log.d("MainActivity", "Showing Auth Screen")
                        AuthScreen(
                            auth = auth,
                            biometricAuth = biometricAuth,
                            onLoginSuccess = { token, name ->
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
}

@Composable1
fun AuthScreen(
    auth: FirebaseAuth,
    biometricAuth: BiometricAuth,
    onLoginSuccess: (String, String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ){
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
                    LoginScreen(auth, biometricAuth, onSwitch = { isRegistering = true }, onLoginSuccess)
                }
            }
        }
    }
    }


@Composable1
fun RegisterScreen(auth: FirebaseAuth, onSwitch: () -> Unit, onRegistrationSuccess: (String) -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val enableBiometric = remember { mutableStateOf(false) }
    val context = LocalContext.current
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = enableBiometric.value, onCheckedChange = { enableBiometric.value = it })
            Text(stringResource(R.string.enable_biometric))
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
                                // Persist the biometric flag for later biometric logins
                                val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
                                prefs.edit().putBoolean("LAST_USER_BIOMETRIC", enableBiometric.value).apply()
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

@Composable1
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
    val errorEmptyFieldsText = stringResource(R.string.error_empty_fields)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.login), style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(stringResource(R.string.password)) },
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
                                // Save the last user UID and first name for biometric login.
                                val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
                                prefs.edit().putString("LAST_USER_UID", firebaseUser.uid)
                                    .putString("LAST_USER_FIRSTNAME", firstName.value).apply()
                                if (biometricEnabledMap[firebaseUser.uid] == true) {
                                    biometricAuth.authenticate()
                                } else {
                                    fetchUserData(firebaseUser.uid) { user ->
                                        Log.d("LoginScreen", "User data fetched: ${user?.firstName}")
                                        firstName.value = user?.firstName
                                        Log.d("LoginScreen", "First name set to: ${firstName.value}")
                                        onLoginSuccess(firebaseUser.uid, user?.firstName)
                                    }
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
        Button(
            onClick = {
                biometricAuth.authenticate()
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(stringResource(R.string.biometric_authentication))
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSwitch) {
            Text(stringResource(R.string.dont_have_account))
        }
    }
}
