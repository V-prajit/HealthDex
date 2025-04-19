package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    // Store string resources in local variables
    val emailRequiredMessage = stringResource(R.string.error_empty_email)
    val passwordResetSentMessage = stringResource(R.string.password_reset_email_sent)
    val passwordResetFailedMessage = stringResource(R.string.password_reset_failed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forgot_password)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.forgot_password_description),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isSuccess
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            message = emailRequiredMessage
                            return@Button
                        }

                        isLoading = true
                        message = ""

                        Log.d("ForgotPassword", "Attempting to send reset email to: $email")
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    isSuccess = true
                                    message = passwordResetSentMessage
                                    Log.d("ForgotPassword", "Password reset email sent successfully")
                                } else {
                                    message = task.exception?.message ?: passwordResetFailedMessage
                                    Log.e("ForgotPassword", "Failed to send reset email: ${task.exception}")
                                    // Print the full stack trace for more details
                                    task.exception?.printStackTrace()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    enabled = !isLoading && !isSuccess
                ) {
                    Text(stringResource(R.string.send_reset_link))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            if (isSuccess) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBackClick) {
                    Text(stringResource(R.string.back_to_login))
                }
            }
        }
    }
}