package com.example.phms

import android.content.Context
import android.os.CancellationSignal
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat

class BiometricAuth(private val context: Context, private val authCallback: (Boolean) -> Unit) {
    private val authentication = FirebaseAuth.getInstance() //gets the current user logged into firebase
    private var cancellationSignal: CancellationSignal? = null //lets user cancel bio authentication
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal!!.setOnCancelListener {
            Toast.makeText(context, "Biometric Authentication Cancelled. Please check again!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "No biometrics enrolled. Check settings.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(context, "Device doesn't support biometrics.", Toast.LENGTH_SHORT).show()
                //covers case if hardware is old
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(context, "Biometric hardware unavailable. Try again later.", Toast.LENGTH_SHORT).show()
                //sometimes hardware is unavailable (being used by another app)- case covers that
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Toast.makeText(context, "Update required to enable biometrics.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)
        //to run on main thread
        val activity = (context as? androidx.fragment.app.FragmentActivity) ?: return
        val biometricPrompt = BiometricPrompt(activity, executor, biometricCallback)
        val promptInformation = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use face recognition, fingerprint or other supported biometric login")
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt.authenticate(promptInformation)
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Toast.makeText(context, "Biometric Authentication Successful!", Toast.LENGTH_SHORT).show()
                Toast.makeText(context, "Welcome : ${user.email}", Toast.LENGTH_SHORT).show()
                authCallback(true)
                //if user found
            } else {
                Toast.makeText(context, "User not found on firebase!", Toast.LENGTH_SHORT).show()
                authCallback(false)
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Toast.makeText(context, "Biometric Authentication Failed", Toast.LENGTH_SHORT).show()
            authCallback(false)
            //authentication failed case, displays appropraite message
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Toast.makeText(context, "Authentication Error: $errString", Toast.LENGTH_SHORT).show()
            authCallback(false)
        }
    }

    fun authenticateFace() {
        val bioManager = BiometricManager.from(context)
        when (bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(context, "No biometrics enrolled. Check settings.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(context, "Device doesn't support biometrics.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(context, "Biometric hardware unavailable. Try again later.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Toast.makeText(context, "Update required to enable biometrics.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
