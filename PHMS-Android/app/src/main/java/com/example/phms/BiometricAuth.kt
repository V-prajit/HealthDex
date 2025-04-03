package com.example.phms

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat
import androidx.core.os.CancellationSignal // <-- Using androidx.core.os.CancellationSignal

class BiometricAuth(private val context: Context, private val authCallback: (Boolean, String?) -> Unit) {
    private val authentication = FirebaseAuth.getInstance() //gets the current user logged into firebase
    private var cancellationSignal: CancellationSignal? = null //lets user cancel bio authentication
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal!!.setOnCancelListener {
            Toast.makeText(context, context.getString(R.string.biometric_cancelled), Toast.LENGTH_SHORT).show()
            // it displays the message- for a short duation of time (about 2 secs is the default) if the user presses cancel
            //basically function runs if cancellationsignal is not null(ie user pressed cancel)
        }
        return cancellationSignal!!
    }

    fun authenticate() {
        val bioManager = BiometricManager.from(context)
        when (bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
            }
            //function checks if biometric auth is possible and if it is proceeds to the appropriate function
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(context, context.getString(R.string.no_biometrics_enrolled), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(context, context.getString(R.string.device_no_biometrics), Toast.LENGTH_SHORT).show()
                //covers case if hardware is old
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(context, context.getString(R.string.biometric_unavailable), Toast.LENGTH_SHORT).show()
                //sometimes hardware is unavailable (being used by another app)- case covers that
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Toast.makeText(context, context.getString(R.string.biometric_update_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)
        // Casting the context to a FragmentActivity (this is required by BiometricPrompt).
        val activity = (context as? androidx.fragment.app.FragmentActivity) ?: return
        val biometricPrompt = BiometricPrompt(activity, executor, biometricCallback)
        // dialog will appear to the user.
        val promptInformation = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login))
            .setSubtitle(context.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()
        // Minimal change: Use the simpler authenticate() call without passing a CancellationSignal.
        biometricPrompt.authenticate(promptInformation)
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            // Retrieve the last login UID (with bio login enabled) from SharedPreferences or use the current Firebase user UID.
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val lastUserUid = prefs.getString("LAST_USER_UID", null) ?: FirebaseAuth.getInstance().currentUser?.uid
            if (lastUserUid != null) {
                fetchUserData(lastUserUid) { userData ->
                    // Check if the fetched user record has biometricEnabled true
                    if (userData != null && userData.biometricEnabled) {
                        Toast.makeText(context, context.getString(R.string.biometric_success), Toast.LENGTH_SHORT).show()
                        Toast.makeText(context, context.getString(R.string.welcome_user, userData.firstName), Toast.LENGTH_SHORT).show()
                        authCallback(true, userData.firstName)
                    } else {
                        Toast.makeText(context, "Biometric login not enabled for this user", Toast.LENGTH_SHORT).show()
                        authCallback(false, null)
                    }
                }
            } else {
                Toast.makeText(context, context.getString(R.string.user_not_found), Toast.LENGTH_SHORT).show()
                authCallback(false, null)
            }
        }
        /**
         * Called when a biometric is valid but not recognized or if the user fails to authenticate
         * with the biometric sensor (e.g., an incorrect fingerprint, partial face match, etc.).
         */
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Toast.makeText(context, context.getString(R.string.biometric_failed), Toast.LENGTH_SHORT).show()
            authCallback(false, null)
            //authentication failed case, displays appropraite message
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Toast.makeText(context, context.getString(R.string.auth_error, errString), Toast.LENGTH_SHORT).show()
            authCallback(false, null)
        }
    }
}
