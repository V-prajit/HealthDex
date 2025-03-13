package com.example.phms

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat

class BiometricAuth(private val context:Context,private val authCallback: (Boolean)->Unit){
    private val authentication= FirebaseAuth.getInstance //gets the current user logged into firebase
    private var cancellationSignal: CancellationSignal? = null //lets user cancel bio authentication
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private fun getCancellationSignal(): CancellationSignal{
        cancellationSignal= CancellationSignal()
        cancellationSignal!!.setOnCancelListener {
            Toast.makeText(context, "Biometric Authentication Cancelled. Please check again!", Toast.LENGTH_SHORT).show()
            // it displays the message- for a short duation of time (about 2 secs is the default) if the user presses cancel
            //basically function runs if cancellationsignal is not null(ie user pressed cancel)
        }
        return cancellationSignal!!
        
    }
}

