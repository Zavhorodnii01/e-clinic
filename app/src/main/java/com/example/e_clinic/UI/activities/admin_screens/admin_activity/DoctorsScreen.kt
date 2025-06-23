package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import kotlin.compareTo
import kotlin.text.clear
import kotlin.text.get


@Composable
fun DoctorsScreen() {
    val doctors = remember { mutableStateListOf<Doctor>() }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showCreateDoctorScreen by remember { mutableStateOf(false) }
    var showTimeslotManager by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val snapshot = firestore.collection("doctors").get().await()
            doctors.clear()
            for (document in snapshot.documents) {
                val doctor = Doctor(
                    id = document.id,
                    name = document.getString("name") ?: "Unknown",
                    surname = document.getString("surname") ?: "Unknown",
                    gender = document.getString("gender") ?: "Unknown",
                    phone = document.getString("phone") ?: "Unknown",
                    email = document.getString("e-mail") ?: "Unknown",
                    specialization = document.getString("specialization") ?: "Unknown",
                    address = document.getString("address") ?: "Unknown",
                    experience = document.getString("experience")?: "0",
                    profilePicture = document.getString("profilePicture") ?: "",
                )
                doctors.add(doctor)
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
                text = "Manage Doctors",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(doctors) { doctor ->
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
            )  {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Front side
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!doctor.profilePicture.isNullOrEmpty()) {
                                AsyncImage(
                                    model = doctor.profilePicture,
                                    contentDescription = "Doctor Avatar",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Doctor",
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "${doctor.name} ${doctor.surname} (${doctor.gender})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Phone: ${doctor.phone}")
                                Text(text = "Email: ${doctor.email}")
                                Text(text = "Specialization: ${doctor.specialization}")
                                Text(text = "Address: ${doctor.address}")
                                Text(text = "Experience: ${doctor.experience} years")
                            }
                        }
                    } else {
                        // Back side
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        ) {
                            Text(
                                text = "Options",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                                textAlign = TextAlign.Start
                            )
                            Button(onClick = {
                                showDialog = true
                                selectedDoctor = doctor
                            }) {
                                Text("Modify Doctor Data")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                showTimeslotManager = true
                                selectedDoctor = doctor
                            }) {
                                Text("Manage Timeslots")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showDeleteDialog = true
                                    selectedDoctor = doctor
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


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showCreateDoctorScreen = true },
            containerColor = Color.Blue,
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(50.dp),
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }

    if (showTimeslotManager && selectedDoctor != null) {
        TimeslotManagerScreen(selectedDoctor!!)
    }

    if (showDialog && selectedDoctor != null) {
        DataManagerScreen(selectedDoctor!!.id, "doctor")
    }


    if (showDeleteDialog && selectedDoctor != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedDoctor!!.name} ${selectedDoctor!!.surname}?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseFirestore.getInstance().collection("doctors").document(selectedDoctor!!.id).delete()
                    doctors.remove(selectedDoctor)
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateDoctorScreen) {
        NewDoctorScreen()
    }
}