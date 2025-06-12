package com.example.e_clinic.Firebase.storage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.e_clinic.CSV.collections.Drug
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.e_clinic.Firebase.collections.Prescription
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream




fun generatePrescription(medication: Drug, dosage: String, quantity: String, prescription: Prescription,
                         doctorName: String,
                         doctorPhone: String,
                         patientName: String): ByteArray {
    // Create a larger bitmap for better quality
    val width = 800
    val height = 1200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Fill background with white
    val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

    // Header with clinic info
    val headerPaint = Paint().apply {
        color = Color.BLUE
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("PRESCRIPTION", width / 2f, 60f, headerPaint)

    val subHeaderPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 16f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("123 Medical Drive, Cityville | Phone: (555) 123-4567", width / 2f, 90f, subHeaderPaint)

    // Divider line
    val linePaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 2f
    }
    canvas.drawLine(50f, 110f, width - 50f, 110f, linePaint)

    // Main content paint
    val mainPaint = Paint().apply {
        color = Color.BLACK
        textSize = 18f
    }

    val boldPaint = Paint(mainPaint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val issuedDate = prescription.issued_date?.toDate()?.let { dateFormat.format(it) } ?: "N/A"


    // Doctor and patient info

    canvas.drawText("Doctor: $doctorName", 50f, 150f, mainPaint)
    canvas.drawText("Phone Number: $doctorPhone", 50f, 180f, mainPaint)
    canvas.drawText("Date of Issue: $issuedDate", width - 250f, 150f, mainPaint)

    canvas.drawText("Patient: $patientName", 50f, 220f, mainPaint)

    // Medication details
    canvas.drawText("Drug name: ${medication.name}+${medication.typeOfPrescription}", 50f, 300f, boldPaint)
    canvas.drawText("Active substance: ${medication.activeSubstance}", 50f, 340f, mainPaint)
    canvas.drawText("Dosage: ${dosage}", 50f, 370f, mainPaint)
    canvas.drawText("Quantity: ${quantity}", 50f, 400f, mainPaint)
    canvas.drawText("Form: ${medication.form}", 50f, 430f, mainPaint)

    // Instructions
    canvas.drawText("Instructions: " + prescription.doctor_comment, 50f, 480f, mainPaint)

    // Generate a simple barcode (simulated)
    val barcodePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
    }
    val barcodeStartY = 550f
    val barcodeHeight = 60f
    // Draw random lines to simulate barcode
    var xPos = 100f
    repeat(50) {
        val lineHeight = (10..barcodeHeight.toInt()).random().toFloat()
        canvas.drawLine(xPos, barcodeStartY, xPos, barcodeStartY + lineHeight, barcodePaint)
        xPos += (3..8).random().toFloat()
    }
    // Barcode number
    canvas.drawText("RX${(100000..999999).random()}", 100f, barcodeStartY + barcodeHeight + 30f, mainPaint)

    // Doctor signature area
    canvas.drawText("_________________________", 50f, 700f, mainPaint)
    canvas.drawText("$doctorName", 50f, 730f, mainPaint)

    // Footer
    val footerPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }
    val validityText = if (medication.typeOfPrescription == "Rpw") {
        "Prescription valid for 30 days from the date of issue"
    } else {
        "Prescription valid for 6 months from the date of issue"
    }
    canvas.drawText(validityText, width / 2f, 800f, subHeaderPaint)
    canvas.drawText("For questions, please call (555) 123-4567", width / 2f, 830f, footerPaint)

    // Convert to byte array
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun uploadPrescriptionToStorage(prescription: Prescription, dosage: String,quantity: String, medication: Drug){

    // Get user and doctor names
    val userId = prescription.user_id ?: "UnknownUser"
    val doctorId = prescription.doctor_id ?: "UnknownDoctor"

    getUserName(userId) { patientName ->
        getDoctorName(doctorId) { doctorName ->
            getDoctorPhone(doctorId) { doctorPhone ->
                val byteArray = generatePrescription(medication, dosage, quantity, prescription, doctorName, doctorPhone, patientName)
                val storageRef = FirebaseStorage.getInstance().reference
                val fileName = "${prescription.id}_${System.currentTimeMillis()}.png"
                val photoRef = storageRef.child("prescriptions/$fileName")

                photoRef.putBytes(byteArray).addOnSuccessListener { uploadTask ->
                    photoRef.downloadUrl.addOnSuccessListener { uri ->
                        // 1. Get Firestore instance
                        val db = FirebaseFirestore.getInstance()

                        // 2. Create a batch operation
                        val batch = db.batch()

                        // 3. Create a new document reference
                        val prescriptionRef = db.collection("prescriptions").document() // Auto-generate ID

                        // 4. Update the prescription object with the URL
                        prescription.link_to_storage = uri.toString()

                        // 5. Add the set() operation to the batch
                        batch.set(prescriptionRef, prescription)

                        // 6. Commit the batch
                        batch.commit().addOnSuccessListener {
                            println("Success: Prescription and photo stored!")
                        }.addOnFailureListener { e ->
                            println("Failed to save prescription: ${e.message}")
                            // Optional: Delete the uploaded photo if Firestore fails
                            photoRef.delete()
                        }
                    }
                }.addOnFailureListener { e ->
                    println("Failed to upload photo: ${e.message}")
                }
            }
        }
    }


    //val byteArray = generatePrescription(medication, dosage, quantity, prescription, doctorName, doctorPhone, patientName)

//    val storageRef = FirebaseStorage.getInstance().reference
//    val fileName = "${prescription.id}_${System.currentTimeMillis()}.png"
//    val photoRef = storageRef.child("prescriptions/$fileName")
//
//    photoRef.putBytes(byteArray).addOnSuccessListener { uploadTask ->
//        photoRef.downloadUrl.addOnSuccessListener { uri ->
//            // 1. Get Firestore instance
//            val db = FirebaseFirestore.getInstance()
//
//            // 2. Create a batch operation
//            val batch = db.batch()
//
//            // 3. Create a new document reference
//            val prescriptionRef = db.collection("prescriptions").document() // Auto-generate ID
//
//            // 4. Update the prescription object with the URL
//            prescription.link_to_storage = uri.toString()
//
//            // 5. Add the set() operation to the batch
//            batch.set(prescriptionRef, prescription)
//
//            // 6. Commit the batch
//            batch.commit().addOnSuccessListener {
//                println("Success: Prescription and photo stored!")
//            }.addOnFailureListener { e ->
//                println("Failed to save prescription: ${e.message}")
//                // Optional: Delete the uploaded photo if Firestore fails
//                photoRef.delete()
//            }
//        }
//    }.addOnFailureListener { e ->
//        println("Failed to upload photo: ${e.message}")
//    }
}



private fun getUserName(userId: String, callback: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val name = document.getString("name") ?: "Unknown User"
                val surname = document.getString("surname") ?: "User"
                callback("$name $surname")
            } else {
                callback("Unknown User")
            }
        }
        .addOnFailureListener {
            callback("Unknown User")
        }
}

private fun getDoctorName(doctorId: String, callback: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("doctors").document(doctorId).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val name = "${document.getString("name")} ${document.getString("surname")}"
                callback(name)
            } else {
                callback("Unknown Doctor")
            }
        }
        .addOnFailureListener {
            callback("Unknown Doctor")
        }
}

private fun getDoctorPhone(doctorId: String, callback: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("doctors").document(doctorId).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val phone = document.getString("phone") ?: "N/A"
                callback(phone)
            } else {
                callback("N/A")
            }
        }
        .addOnFailureListener {
            callback("N/A")
        }
}

// Example usage (in an Activity or Fragment, after you have width and height):
//val prescriptionBytes = PrescriptionGenerator.generatePrescription(viewWidth, viewHeight)
// Upload prescriptionBytes to Firebase Storage