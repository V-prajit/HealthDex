package com.example.phms.Screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.phms.R
import com.example.phms.RetrofitClient
import com.example.phms.SecurityQuestions
import com.example.phms.UserDTO
import com.example.phms.VerificationResponse
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var userFound by remember { mutableStateOf(false) }
    var securityQuestion by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var securityQuestionId by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val emailRequiredMessage = stringResource(R.string.error_empty_email)
    val passwordResetSentMessage = stringResource(R.string.password_reset_email_sent)
    val passwordResetFailedMessage = stringResource(R.string.password_reset_failed)
    val securityAnswerRequiredMessage = stringResource(R.string.error_security_answer_required)
    val securityAnswerWrongMessage = stringResource(R.string.error_security_answer_incorrect)
    val userNotFoundMessage = stringResource(R.string.error_user_not_found_email)
    val findAccountErrorTemplate = stringResource(R.string.error_find_account)
    val verifyErrorTemplate = stringResource(R.string.error_verify_security)
    val noErrorDetailsText = stringResource(R.string.error_no_details)

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

            if (!userFound) {
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            message = emailRequiredMessage
                            return@Button
                        }

                        isLoading = true
                        message = ""

                        Log.d("ForgotPassword", "Finding user with email: $email")


                        RetrofitClient.apiService.findUserByEmail(email).enqueue(object : Callback<UserDTO> {
                            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                                isLoading = false
                                Log.d("ForgotPassword", "Response code: ${response.code()}")

                                if (response.isSuccessful && response.body() != null) {
                                    val user = response.body()!!
                                    Log.d("ForgotPassword", "User found: ${user.firebaseUid}")
                                    userId = user.firebaseUid
                                    securityQuestionId = user.securityQuestionId ?: 1
                                    securityQuestion = SecurityQuestions.questions.find { it.id == securityQuestionId }?.question ?: ""
                                    userFound = true
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: noErrorDetailsText
                                    Log.e("ForgotPassword", "Error finding user: $errorBody")
                                    message = userNotFoundMessage
                                }
                            }

                            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                                isLoading = false
                                Log.e("ForgotPassword", "Request failed", t)
                                message = String.format(findAccountErrorTemplate, t.message)
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    enabled = !isLoading && !isSuccess && !userFound
                ) {
                    Text(stringResource(R.string.find_account_button))
                }
            } else {

                Text(
                    text = stringResource(R.string.security_question_title),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = securityQuestion,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = { securityAnswer = it },
                    label = { Text(stringResource(R.string.your_answer_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !isSuccess
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (securityAnswer.isBlank()) {
                            message = securityAnswerRequiredMessage
                            return@Button
                        }

                        isLoading = true
                        message = ""

                        RetrofitClient.apiService.verifySecurityAnswer(userId, securityQuestionId, securityAnswer)
                            .enqueue(object : Callback<VerificationResponse> {
                                override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                                    if (response.isSuccessful && response.body()?.verified == true) {

                                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    isSuccess = true
                                                    message = passwordResetSentMessage
                                                } else {
                                                    message = task.exception?.message ?: passwordResetFailedMessage
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        message = securityAnswerWrongMessage
                                    }
                                }

                                override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                                    isLoading = false
                                    message = String.format(verifyErrorTemplate, t.message)
                                }
                            })
                    },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    enabled = !isLoading && !isSuccess
                ) {
                    Text(stringResource(R.string.send_reset_link))
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
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