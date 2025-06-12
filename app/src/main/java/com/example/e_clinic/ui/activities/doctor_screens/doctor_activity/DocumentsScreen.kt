package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.e_clinic.Firebase.collections.Prescription
import com.google.firebase.auth.FirebaseAuth
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreenForm(doctorId: String) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Prescriptions")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PrescriptionTab(doctorId)
            }
        }
    }
}

@Composable
fun PrescriptionTab(doctorId: String) {
    val db = FirebaseFirestore.getInstance()
    val prescriptions = remember { mutableStateListOf<Prescription>() }

    LaunchedEffect(doctorId) {
        db.collection("prescriptions")
            .whereEqualTo("doctor_id", doctorId)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.e("PrescriptionTab", "No prescriptions found for doctorId: $doctorId")
                } else {
                    prescriptions.clear()
                    prescriptions.addAll(result.toObjects(Prescription::class.java))
                    Log.d("PrescriptionTab", "Prescriptions fetched: ${prescriptions.size}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PrescriptionTab", "Failed to fetch prescriptions: ${e.message}")
            }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(prescriptions, key = { it.id }) { prescription ->
            PrescriptionItem(prescription)
        }
    }
}

@Composable
fun PrescriptionItem(prescription: Prescription) {
    val db = FirebaseFirestore.getInstance()
    val patientName = remember { mutableStateOf("Loading...") }
    var showDialog by remember { mutableStateOf(false) }
    var showQRCodeDialog by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }

    LaunchedEffect(prescription.user_id) {
        if (prescription.user_id.isNotEmpty()) {
            db.collection("users").document(prescription.user_id).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "Unknown"
                    val surname = document.getString("surname") ?: "User"
                    patientName.value = "$name $surname"
                }
                .addOnFailureListener {
                    patientName.value = "Unknown User"
                }
        } else {
            patientName.value = "Unknown User"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { /* Handle click to show details */ },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Patient: ${patientName.value}", style = MaterialTheme.typography.bodyMedium)
            Text("Date of Issue: ${prescription.issued_date?.toDate()}", style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(prescription.link_to_storage)
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        imageUrl = uri.toString()
                        showDialog = true
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("View Prescription")
            }
            Button(
                onClick = {
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(prescription.link_to_storage)
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        imageUrl = uri.toString()
                        showQRCodeDialog = true
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Show QR Code")
            }
        }
    }
    if (showDialog) {
        showImageDialog(imageUrl)
    }
    if (showQRCodeDialog) {
        showQRCodeDialog(imageUrl){
            showQRCodeDialog = false
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen() {
    val auth = FirebaseAuth.getInstance()
    val doctorEmail = auth.currentUser?.email ?: ""
    val doctorId = remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(doctorEmail) {
        if (doctorEmail.isNotEmpty()) {
            db.collection("doctors").whereEqualTo("e-mail", doctorEmail).get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    doctorId.value = documents.documents[0].id
                }
            }
        }
    }

    Scaffold(
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            doctorId.value?.let { id ->
                DocumentScreenForm(doctorId = id)
            }
        }
    }
}

@Composable
fun showImageDialog(imageUrl: String) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Prescription Image") },
            text = {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = "Prescription Image",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun showQRCodeDialog(link: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR Code") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val qrCodeBitmap = generateQRCode(link)
                qrCodeBitmap?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun generateQRCode(link: String): Bitmap? {
    return try {
        val size = 512 // Size of the QR code
        val qrCodeWriter = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(link, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}