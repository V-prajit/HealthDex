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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context.CONTEXT_INCLUDE_CODE
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.lifecycleScope
import com.example.phms.network.NutritionRetrofit
import com.example.phms.repository.NutritionRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

val biometricEnabledMap = mutableMapOf<String, Boolean>()

class MainActivity : FragmentActivity() {
    private lateinit var auth: FirebaseAuth

    private var localeVersion by mutableIntStateOf(0)
    private var darkModeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        NutritionRepository.init(this)
        Log.d("FDC-KEY", BuildConfig.FDC_API_KEY)

        lifecycleScope.launch {
            try {
                val foods = NutritionRetrofit.service.searchFoods("apple").foods
                Log.d("FDC-TEST", "Hits = ${foods.size}")
                foods.firstOrNull()?.let { Log.d("FDC-TEST", "First hit = ${it.description}") }
            } catch (e: Exception) {
                Log.e("FDC-TEST", "API call failed", e)
            }
        }

        auth = FirebaseAuth.getInstance()

        auth.signOut()
        AppointmentReminderWorker.initialize(this)

        val appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!appPrefs.contains("has_launched_before")) {
            appPrefs.edit().putBoolean("has_launched_before", true).apply()
        }

        val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!userPrefs.contains("locale_set")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            userPrefs.edit().putBoolean("locale_set", true).apply()
        }

        darkModeEnabled = userPrefs.getBoolean("DARK_MODE", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContent {
            val currentDarkMode = this@MainActivity.darkModeEnabled 
            val currentLocaleVersion = this@MainActivity.localeVersion 

            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)

            PHMSTheme(darkTheme = currentDarkMode) {
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) } 
                var userToken by remember { mutableStateOf(auth.currentUser?.uid) }
                var firstName by remember { mutableStateOf<String?>(null) }
                var showSettings by remember { mutableStateOf(false) }
                var returnToTab by remember { mutableStateOf<String?>(null) }

              
                val biometricAuth = remember {
                     BiometricAuth(this@MainActivity) { success, name ->
                        if (success) {                      
                             val savedUid = prefs.getString("LAST_USER_UID", null)
                             val savedFirstName = prefs.getString("LAST_USER_FIRSTNAME", null)
                            userToken = savedUid
                            isLoggedIn = true
                            firstName = name ?: savedFirstName
                        }
                    }
                }


                LaunchedEffect(auth.currentUser) { 
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                         if (userToken != currentUser.uid || firstName == null) {
                             Log.d("MainActivity", "LaunchedEffect: User logged in (${currentUser.uid}), fetching data.")
                             userToken = currentUser.uid
                             fetchUserData(currentUser.uid) { userData ->
                                 firstName = userData?.firstName
                                 isLoggedIn = true
                             }
                        }
                    } else {
                         Log.d("MainActivity", "LaunchedEffect: No user logged in.")
                    }
                }


                LaunchedEffect(showSettings) {
                    if (!showSettings) {
                        returnToTab = null
                    }
                }

                Log.d(
                    "MainActivity",
                    "Rendering: isLoggedIn=$isLoggedIn, showSettings=$showSettings, returnToTab=$returnToTab, localeVersion=$currentLocaleVersion"
                )
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
                                returnToTab = null
                                // Consider clearing relevant prefs on logout
                                prefs.edit().remove("LAST_USER_FIRSTNAME").apply()
                            }
                        )
                    }

                    isLoggedIn -> {
                        Log.d("MainActivity", "Showing Dashboard Screen")
                        val currentReturnToTab = returnToTab
                        DashboardScreen(
                            firstName = firstName,
                            userToken = userToken,
                            initialSelectedTab = currentReturnToTab ?: "home",
                            onSettingsClick = { originTab ->
                                Log.d("MainActivity", "Settings button clicked from: $originTab")
                                showSettings = true
                                returnToTab = originTab
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
                            onSettingsClick = {
                                showSettings = true
                                returnToTab = null 
                            }
                        )
                    }
                }
            }
        }
    }

    // Function to update theme - MODIFIED
    fun updateTheme(darkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        this.darkModeEnabled = darkMode

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("DARK_MODE", darkMode).apply()

    }

    fun forceLocaleRecomposition(languageCode: String? = null) {
        localeVersion++
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
        var isRegistering by remember { mutableStateOf(false) }
        var showForgotPassword by remember { mutableStateOf(false) }
        var userToken by remember { mutableStateOf<String?>(null) } 
        var showUserDetailsScreen by remember { mutableStateOf(false) }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp), 
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                }
            }
            when {
                showForgotPassword -> {
                    ForgotPasswordScreen(onBackClick = { showForgotPassword = false })
                }
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
                    LoginScreen(
                        auth = auth,
                        biometricAuth = biometricAuth,
                        onSwitch = { isRegistering = true },
                        onLoginSuccess = onLoginSuccess,
                        onForgotPassword = { showForgotPassword = true}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable1
fun RegisterScreen(auth: FirebaseAuth, onSwitch: () -> Unit, onRegistrationSuccess: (String) -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val enableBiometric = remember { mutableStateOf(false) }
    val selectedQuestionIndex = remember { mutableStateOf(0) }
    val securityAnswer = remember { mutableStateOf("") }
    val expanded = remember { mutableStateOf(false) }
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Security Question Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = it }
        ) {
            OutlinedTextField(
                value = SecurityQuestions.questions[selectedQuestionIndex.value].question,
                onValueChange = {},
                readOnly = true,
                label = { Text("Security Question") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                SecurityQuestions.questions.forEachIndexed { index, question ->
                    DropdownMenuItem(
                        text = { Text(question.question) },
                        onClick = {
                            selectedQuestionIndex.value = index
                            expanded.value = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Security Answer Input
        OutlinedTextField(
            value = securityAnswer.value,
            onValueChange = { securityAnswer.value = it },
            label = { Text("Security Answer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = enableBiometric.value, onCheckedChange = { enableBiometric.value = it })
            Text(stringResource(R.string.enable_biometric)) // Assuming R.string.enable_biometric
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.value.isBlank() || password.value.isBlank()) {
                    message.value = "Email and password cannot be empty"
                    return@Button
                }
                 message.value = ""

                auth.createUserWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            if (firebaseUser != null) {
                                val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
                                prefs.edit()
                                    .putBoolean("LAST_USER_BIOMETRIC", enableBiometric.value)
                                    .putString("LAST_USER_UID", firebaseUser.uid)
                                    .putInt("SECURITY_QUESTION_ID", SecurityQuestions.questions[selectedQuestionIndex.value].id)
                                    .putString("SECURITY_ANSWER", securityAnswer.value)
                                    .apply()

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
            // Assuming R.string.already_have_account
            Text(stringResource( R.string.already_have_account))
        }
    }
}


@Composable1
fun LoginScreen(
    auth: FirebaseAuth,
    biometricAuth: BiometricAuth,
    onSwitch: () -> Unit,
    onLoginSuccess: (String, String?) -> Unit,
    onForgotPassword: () -> Unit
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val firstName = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val errorEmptyFieldsText = stringResource(R.string.error_empty_fields)
    val loginFailedTemplate = stringResource(R.string.login_failed)
    var passwordVisible by remember { mutableStateOf(false) }

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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onForgotPassword) {
            Text(stringResource(R.string.forgot_password))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
        Button(
            onClick = {
                if (email.value.isBlank() || password.value.isBlank()) {
                    message.value =  errorEmptyFieldsText
                    return@Button
                }
                 message.value = ""

                auth.signInWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            if (firebaseUser != null) {
                                Log.d("LoginScreen", "Login successful, fetching user data for ${firebaseUser.uid}")

                                val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
                                fetchUserData(firebaseUser.uid) { user ->
                                    Log.d("LoginScreen", "User data fetched: ${user?.firstName}")
                                    firstName.value = user?.firstName // Update local state (though might be reset on nav)
                                    val isBiometricEnabledForUser = user?.biometricEnabled ?: false // Assuming fetchUserData returns this

                                    prefs.edit()
                                        .putString("LAST_USER_UID", firebaseUser.uid)
                                        .putString("LAST_USER_FIRSTNAME", firstName.value) 
                                        .putBoolean("LAST_USER_BIOMETRIC", isBiometricEnabledForUser)
                                        .apply()
                                    Log.d("LoginScreen", "Saved UID, Name, Biometric flag after data fetch.")
                                    if (biometricEnabledMap[firebaseUser.uid] == true) {
                                         Log.d("LoginScreen", "Biometric enabled in map, authenticating...")
                                        biometricAuth.authenticate()
                                    } else {
                                         Log.d("LoginScreen", "Biometric not enabled in map, calling onLoginSuccess.")
                                        onLoginSuccess(firebaseUser.uid, user?.firstName)
                                    }
                                }
                            }
                        } else {
                             Log.w("LoginScreen", "Login failed", task.exception)
                            message.value = String.format(loginFailedTemplate, task.exception?.message ?: "Unknown error")
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
            // Assuming R.string.biometric_authentication
            Text(stringResource(R.string.biometric_authentication))
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary) // Or error color
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSwitch) {
            // Assuming R.string.dont_have_account
            Text(stringResource(R.string.dont_have_account))
        }
    }
}