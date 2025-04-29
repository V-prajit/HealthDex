package com.example.phms

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuth(private val context: Context, private val authCallback: (Boolean, String?) -> Unit) {
    private val authentication = FirebaseAuth.getInstance()
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun authenticate() {
        val bioManager = BiometricManager.from(context)
        when (bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(context, context.getString(R.string.no_biometrics_enrolled), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(context, context.getString(R.string.device_no_biometrics), Toast.LENGTH_SHORT).show()

            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(context, context.getString(R.string.biometric_unavailable), Toast.LENGTH_SHORT).show()

            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Toast.makeText(context, context.getString(R.string.biometric_update_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)

        val activity = (context as? FragmentActivity) ?: return
        val biometricPrompt = BiometricPrompt(activity, executor, biometricCallback)

        val promptInformation = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login))
            .setSubtitle(context.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInformation)
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            val lastUserUid = prefs.getString("LAST_USER_UID", null)

            if (lastUserUid != null) {

                fetchUserData(lastUserUid) { userData ->
                    if (userData != null && userData.biometricEnabled) {

                        prefs.edit()
                            .putString("LAST_USER_UID", lastUserUid)
                            .putBoolean("LAST_USER_BIOMETRIC", true)
                            .apply()

                        Toast.makeText(context, context.getString(R.string.biometric_success), Toast.LENGTH_SHORT).show()
                        Toast.makeText(
                            context,
                            context.getString(R.string.welcome_user, userData.firstName ?: authentication.currentUser?.displayName ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()


                        authCallback(true, userData.firstName ?: authentication.currentUser?.displayName ?: "")
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_biometric_not_enabled),
                            Toast.LENGTH_SHORT
                        ).show()
                        authCallback(false, null)
                    }
                }
            } else {

                val currentUid = authentication.currentUser?.uid
                if (currentUid != null) {
                    fetchUserData(currentUid) { userData ->
                        if (userData != null && userData.biometricEnabled) {

                            prefs.edit()
                                .putString("LAST_USER_UID", currentUid)
                                .putBoolean("LAST_USER_BIOMETRIC", true)
                                .apply()

                            Toast.makeText(context, context.getString(R.string.biometric_success), Toast.LENGTH_SHORT).show()
                            Toast.makeText(
                                context,
                                context.getString(R.string.welcome_user, userData.firstName ?: authentication.currentUser?.displayName ?: ""),
                                Toast.LENGTH_SHORT
                            ).show()

                            authCallback(true, userData.firstName ?: authentication.currentUser?.displayName ?: "")
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_biometric_not_enabled),
                                Toast.LENGTH_SHORT
                            ).show()
                            authCallback(false, null)
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.user_not_found), Toast.LENGTH_SHORT).show()
                    authCallback(false, null)
                }
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Toast.makeText(context, context.getString(R.string.biometric_failed), Toast.LENGTH_SHORT).show()
            authCallback(false, null)

        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode == BiometricPrompt.ERROR_CANCELED || errorCode == BiometricPrompt.ERROR_USER_CANCELED)return
            Toast.makeText(context, context.getString(R.string.auth_error, errString), Toast.LENGTH_SHORT).show()
            authCallback(false, null)
        }
    }
}