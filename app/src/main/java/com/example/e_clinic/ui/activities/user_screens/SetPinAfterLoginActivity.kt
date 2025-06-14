// SetPinAfterLoginActivity.kt
package com.example.e_clinic.ui.activities.user_screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.services.PinManager

class SetPinAfterLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetPinAfterLoginScreen()
        }
    }
}

@Composable
fun SetPinAfterLoginScreen() {
    val context = LocalContext.current
    val pinManager = PinManager(context)

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = if (step == 1) "Set your PIN" else "Confirm your PIN",
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val currentPin = if (step == 1) pin else confirmPin
                val filled = i <= currentPin.length
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 1..3) {
                        val number = (row * 3 + col).toString()
                        NumberButtonSet(number) {
                            if (step == 1 && pin.length < 4) {
                                pin += number
                                if (pin.length == 4) step = 2
                            } else if (step == 2 && confirmPin.length < 4) {
                                confirmPin += number
                                if (confirmPin.length == 4) {
                                    if (pin == confirmPin) {
                                        pinManager.savePin(pin)
                                        val intent = Intent(context, PinEntryActivity::class.java)
                                        context.startActivity(intent)
                                        (context as Activity).finish()
                                    } else {
                                        error = "PINs do not match"
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(64.dp))

                NumberButtonSet("0") {
                    if (step == 1 && pin.length < 4) {
                        pin += "0"
                        if (pin.length == 4) step = 2
                    } else if (step == 2 && confirmPin.length < 4) {
                        confirmPin += "0"
                        if (confirmPin.length == 4) {
                            if (pin == confirmPin) {
                                pinManager.savePin(pin)
                                val intent = Intent(context, PinEntryActivity::class.java)
                                context.startActivity(intent)
                                (context as Activity).finish()
                            } else {
                                error = "PINs do not match"
                                confirmPin = ""
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            if (step == 1 && pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                            } else if (step == 2 && confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⌫",
                        fontSize = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun NumberButtonSet(number: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}