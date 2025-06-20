@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@Composable
fun UsersScreen() {
    val users = remember { mutableStateListOf<User>() }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val snapshot = firestore.collection("users").get().await()
            users.clear()
            for (document in snapshot.documents) {
                val user = User(
                    id = document.id,
                    name = document.getString("name") ?: "Unknown",
                    surname = document.getString("surname") ?: "Unknown",
                    phone = document.getString("phone") ?: "Unknown",
                    email = document.getString("email") ?: "Unknown",
                    gender = document.getString("gender") ?: "Unknown",
                    address = document.getString("address") ?: "Unknown")
                users.add(user)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Manage Patients",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(users) { user ->
            var isFlipped by remember { mutableStateOf(false) }
            val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        isFlipped = !isFlipped
                    }
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f){
                        Text(
                            text = "${user.name} ${user.surname}(${user.gender})\nEmail: ${user.email}\nAddress: ${user.address} \n",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    else {
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        ) {
                            Text(
                                text = "Options ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = {
                                    showDialog = true
                                    selectedUser = user
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                            ) {
                                Text("Modify User Data", color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showDeleteDialog = true
                                    selectedUser = user
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Remove", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDialog && selectedUser != null) {
       DataManagerScreen(selectedUser!!.id, "user")
    }
    if (showDeleteDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete this user?") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance().collection("users").document(selectedUser!!.id).delete()
                        users.remove(selectedUser)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}
