package com.example.phms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun UserDetailsScreen(userToken: String?, onDetailsSubmitted: (String, String?) -> Unit ) {
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val age = remember { mutableStateOf("") }
    val height = remember { mutableStateOf("") }
    val weight = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val biometricEnabled = remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser
    val userEmail = user?.email ?: ""
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    LaunchedEffect(userToken) {
        if (userToken != null) {
            fetchUserData(userToken) { userData ->
                if (userData != null) {
                    firstName.value = userData.firstName
                    lastName.value = userData.lastName
                    age.value = userData.age?.toString() ?: ""
                    height.value = userData.height?.toString() ?: ""
                    weight.value = userData.weight?.toString() ?: ""
                    biometricEnabled.value = userData.biometricEnabled
                }
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.user_details), style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = firstName.value,
            onValueChange = { firstName.value = it },
            label = { Text(stringResource(R.string.first_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = lastName.value,
            onValueChange = { lastName.value = it },
            label = { Text(stringResource(R.string.last_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = age.value,
            onValueChange = { age.value = it },
            label = { Text(stringResource(R.string.age)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = height.value,
            onValueChange = { height.value = it },
            label = { Text(stringResource(R.string.height)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = weight.value,
            onValueChange = { weight.value = it },
            label = { Text(stringResource(R.string.weight)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (biometricEnabled.value) {
                    prefs.edit().putString("LAST_USER_UID", userToken).putBoolean("LAST_USER_BIOMETRIC", true).apply()
                }
                val updatedBiometric = prefs.getBoolean("LAST_USER_BIOMETRIC", false)
                sendUserDataToBackend(
                    userToken,
                    userEmail,
                    firstName.value,
                    lastName.value,
                    age.value,
                    height.value,
                    weight.value,
                    updatedBiometric
                ) {
                    message.value = it
                    if (userToken != null){
                        fetchUserData(userToken) { userData ->
                            onDetailsSubmitted(userToken, userData?.firstName)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(stringResource(R.string.submit))
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }
    }
}
