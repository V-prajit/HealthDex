package com.example.phms

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.phms.Screens.DashboardScreen
import com.example.phms.Screens.ForgotPasswordScreen
import com.example.phms.Screens.SettingScreen
import com.example.phms.Screens.UserDetailsScreen
import com.example.phms.network.NutritionRetrofit
import com.example.phms.repository.NutritionRepository
import com.example.phms.ui.theme.PHMSTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable as Composable1


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
        val isFirstLaunch = !appPrefs.contains("has_launched_before")

        if (isFirstLaunch) {
            // Don't set has_launched_before yet - we'll set it after language selection
        } else {
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
                var showFirstTimeLaunch by remember { mutableStateOf(isFirstLaunch) }

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
                    showFirstTimeLaunch -> {
                        FirstTimeLanguageScreen {
                            showFirstTimeLaunch = false
                            appPrefs.edit().putBoolean("has_launched_before", true).apply()
                        }
                    }
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
                            }
                        )
                    }
                }
            }
        }
    }

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

@Composable
fun FirstTimeLanguageScreen(onLanguageSelected: () -> Unit) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to PHMS",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Please select your language",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                items(LocaleHelper.supportedLanguages) { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LocaleHelper.applyLanguageWithoutRecreation(
                                    context,
                                    language.code
                                )
                                onLanguageSelected()
                            }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (language != LocaleHelper.supportedLanguages.last()) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    biometricAuth: BiometricAuth,
    onLoginSuccess: (String, String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ){
        var isRegistering by remember { mutableStateOf(false) }
        var showForgotPassword by remember { mutableStateOf(false) }
        var userToken by remember { mutableStateOf<String?>(null) }
        var showUserDetailsScreen by remember { mutableStateOf(false) }
        var showLanguageSelector by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val currentLanguageCode = remember { LocaleHelper.getCurrentLanguageCode(context) }
        val currentLanguage = remember(currentLanguageCode) {
            LocaleHelper.supportedLanguages.find { it.code == currentLanguageCode } ?: LocaleHelper.supportedLanguages[0]
        }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showLanguageSelector = true }) {
                    Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language))
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

        if (showLanguageSelector) {
            AlertDialog(
                onDismissRequest = { showLanguageSelector = false },
                title = { Text(stringResource(R.string.select_language)) },
                text = {
                    LazyColumn {
                        items(LocaleHelper.supportedLanguages) { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        LocaleHelper.applyLanguageWithoutRecreation(
                                            context,
                                            language.code
                                        )
                                        showLanguageSelector = false
                                    }
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(language.displayName)

                                if (language.code == currentLanguageCode) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (language != LocaleHelper.supportedLanguages.last()) {
                                Divider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageSelector = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
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

        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = {Text(stringResource(R.string.email))},
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(modifier = Modifier.padding(start = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.password_requirements_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.password_requirement_length),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = it }
        ) {
            OutlinedTextField(
                value = SecurityQuestions.questions[selectedQuestionIndex.value].question,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.security_question_label)) },
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

        OutlinedTextField(
            value = securityAnswer.value,
            onValueChange = { securityAnswer.value = it },
            label = { Text(stringResource(R.string.security_answer_label)) },
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
            Text(stringResource(R.string.enable_biometric))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.value.isBlank() || password.value.isBlank()) {
                    message.value = context.getString(R.string.error_empty_email_password)
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
                            message.value = context.getString(R.string.registration_failed, task.exception?.message ?: "")
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
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onForgotPassword) {
            Text(stringResource(R.string.forgot_password),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


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
                                    firstName.value = user?.firstName
                                    val isBiometricEnabledForUser = user?.biometricEnabled ?: false

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
                            message.value = String.format(loginFailedTemplate, task.exception?.message ?: context.getString(R.string.login_error_unknown))
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(
                stringResource(R.string.login_button),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                biometricAuth.authenticate()
            },
            modifier = Modifier.fillMaxWidth(0.6f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.biometric_authentication),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 22.sp
                )
            }
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