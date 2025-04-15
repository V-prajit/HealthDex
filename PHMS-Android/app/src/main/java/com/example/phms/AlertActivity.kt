package com.example.phms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sign  = intent.getStringExtra("sign") ?: "Vital"
        val value = intent.getStringExtra("value") ?: "?"

        setContent { AlertScreen(sign, value) }
    }

    @Composable
    private fun AlertScreen(sign: String, value: String) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(text = "$sign out of range!", fontSize = 22.sp)
                Text(text = "Reading: $value", fontSize = 18.sp)

                Button(onClick = {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
                }) { Text("Call Doctor") }

                OutlinedButton(onClick = { finish() }) { Text("Dismiss") }
            }
        }
    }
}
