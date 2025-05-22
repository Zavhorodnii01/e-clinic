package com.example.e_clinic.Firebase.storage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.e_clinic.Firebase.collections.Prescription
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream



fun generatePrescription(medication: String, prescription: Prescription): ByteArray {
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
    canvas.drawText("HEALTHCARE CLINIC", width / 2f, 60f, headerPaint)

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

    // Doctor and patient info
    canvas.drawText("Dr. Smith Johnson", 50f, 150f, mainPaint)
    canvas.drawText("License: MD123456", 50f, 180f, mainPaint)
    canvas.drawText("Date: ${prescription.issued_date}", width - 250f, 150f, mainPaint)

    canvas.drawText("Patient: John Doe", 50f, 220f, mainPaint)
    canvas.drawText("DOB: 01/15/1985", 50f, 250f, mainPaint)
    canvas.drawText("Patient ID: ", width - 250f, 220f, mainPaint)

    // Prescription details
    val boldPaint = Paint(mainPaint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("PRESCRIPTION", 50f, 300f, boldPaint)

    // Medication details
    canvas.drawText("Medication: $medication", 50f, 340f, mainPaint)
    canvas.drawText("Dosage: 1 tablet twice daily", 50f, 370f, mainPaint)
    canvas.drawText("Duration: 10 days", 50f, 400f, mainPaint)
    canvas.drawText("Refills: 1", 50f, 430f, mainPaint)

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
    canvas.drawText("Dr. Smith Johnson", 50f, 730f, mainPaint)

    // Footer
    val footerPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("This prescription is valid for 6 months from issue date", width / 2f, 800f, footerPaint)
    canvas.drawText("For questions, please call (555) 123-4567", width / 2f, 830f, footerPaint)

    // Convert to byte array
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
fun uploadPrescriptionToStorage(prescription: Prescription, medication: String) {
    val byteArray = generatePrescription(medication, prescription)

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




// Example usage (in an Activity or Fragment, after you have width and height):
//val prescriptionBytes = PrescriptionGenerator.generatePrescription(viewWidth, viewHeight)
// Upload prescriptionBytes to Firebase Storage