package com.example.phms.ui.features.auth

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
import com.example.phms.R
import com.example.phms.domain.repository.fetchUserData
import com.example.phms.domain.repository.sendUserDataToBackend

@Composable
fun UserDetailsScreen(userToken: String?, onDetailsSubmitted: (String, String?) -> Unit ) {
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val age = remember { mutableStateOf("") }
    val height = remember { mutableStateOf("") }
    val weight = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }

    val hasFirstNameError = remember { mutableStateOf(false) }
    val hasLastNameError = remember { mutableStateOf(false) }
    val hasAgeError = remember { mutableStateOf(false) }
    val hasHeightError = remember { mutableStateOf(false) }
    val hasWeightError = remember { mutableStateOf(false) }

    val biometricEnabled = remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser
    val userEmail = user?.email ?: ""
    val context = LocalContext.current

    val mustBeNumberText = stringResource(R.string.must_be_number)
    val fillAllFieldsText = stringResource(R.string.fill_all_fields)
    val firstNameRequiredText = stringResource(R.string.first_name_required)
    val lastNameRequiredText = stringResource(R.string.last_name_required)
    val ageRequiredText = stringResource(R.string.age_required)
    val heightRequiredText = stringResource(R.string.height_required)
    val weightRequiredText = stringResource(R.string.weight_required)

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
            onValueChange = {
                firstName.value = it
                hasFirstNameError.value = it.isBlank()
            },
            label = { Text(stringResource(R.string.first_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            isError = hasFirstNameError.value,
            supportingText = {
                if (hasFirstNameError.value) {
                    Text(firstNameRequiredText)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lastName.value,
            onValueChange = {
                lastName.value = it
                hasLastNameError.value = it.isBlank()
            },
            label = { Text(stringResource(R.string.last_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            isError = hasLastNameError.value,
            supportingText = {
                if (hasLastNameError.value) {
                    Text(lastNameRequiredText)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = age.value,
            onValueChange = {
                age.value = it
                hasAgeError.value = it.isBlank() || !it.all { char -> char.isDigit() }
            },
            label = { Text(stringResource(R.string.age)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            isError = hasAgeError.value,
            supportingText = {
                if (hasAgeError.value) {
                    if (age.value.isBlank()) {
                        Text(ageRequiredText)
                    } else if (!age.value.all { char -> char.isDigit() }) {
                        Text(mustBeNumberText)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = height.value,
            onValueChange = {
                height.value = it
                hasHeightError.value = it.isBlank() || !it.all { char -> char.isDigit() || char == '.' }
            },
            label = { Text(stringResource(R.string.height)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            isError = hasHeightError.value,
            supportingText = {
                if (hasHeightError.value) {
                    if (height.value.isBlank()) {
                        Text(heightRequiredText)
                    } else if (!height.value.all { char -> char.isDigit() || char == '.' }) {
                        Text(mustBeNumberText)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weight.value,
            onValueChange = {
                weight.value = it
                hasWeightError.value = it.isBlank() || !it.all { char -> char.isDigit() || char == '.' }
            },
            label = { Text(stringResource(R.string.weight)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            isError = hasWeightError.value,
            supportingText = {
                if (hasWeightError.value) {
                    if (weight.value.isBlank()) {
                        Text(weightRequiredText)
                    } else if (!weight.value.all { char -> char.isDigit() || char == '.' }) {
                        Text(mustBeNumberText)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Validate all fields
                hasFirstNameError.value = firstName.value.isBlank()
                hasLastNameError.value = lastName.value.isBlank()
                hasAgeError.value = age.value.isBlank() || !age.value.all { char -> char.isDigit() }
                hasHeightError.value = height.value.isBlank() || !height.value.all { char -> char.isDigit() || char == '.' }
                hasWeightError.value = weight.value.isBlank() || !weight.value.all { char -> char.isDigit() || char == '.' }

                // Only proceed if all fields are valid
                if (!hasFirstNameError.value && !hasLastNameError.value &&
                    !hasAgeError.value && !hasHeightError.value && !hasWeightError.value) {
                    val securityQuestionId = prefs.getInt("SECURITY_QUESTION_ID", 1)
                    val securityAnswer = prefs.getString("SECURITY_ANSWER", "") ?: ""
                    if (biometricEnabled.value) {
                        prefs.edit()
                            .putString("LAST_USER_UID", userToken)
                            .putString("LAST_USER_FIRSTNAME", firstName.value)
                            .putBoolean("LAST_USER_BIOMETRIC", true)
                            .apply()
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
                        updatedBiometric,
                        securityQuestionId,
                        securityAnswer
                    ) {
                        message.value = it
                        if (userToken != null){
                            fetchUserData(userToken) { userData ->
                                onDetailsSubmitted(userToken, userData?.firstName)
                            }
                        }
                    }
                } else {
                    message.value = fillAllFieldsText
                }
            },
            modifier = Modifier.fillMaxWidth(0.6f),
            enabled = firstName.value.isNotBlank() && lastName.value.isNotBlank() &&
                    age.value.isNotBlank() && height.value.isNotBlank() && weight.value.isNotBlank() &&
                    age.value.all { char -> char.isDigit() } &&
                    height.value.all { char -> char.isDigit() || char == '.' } &&
                    weight.value.all { char -> char.isDigit() || char == '.' }
        ) {
            Text(stringResource(R.string.submit))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.value.isNotEmpty()) {
            Text(text = message.value, color = MaterialTheme.colorScheme.primary)
        }
    }
}
