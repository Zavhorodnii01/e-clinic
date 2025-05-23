package com.example.e_clinic.services.functions

import com.example.e_clinic.services.Service

fun appServices(): List<Service> {
    return listOf(
        Service(1, "appointment", "Appointment"),
        Service(2, "prescription", "Your Prescriptions"),
        Service(3, "doctor_chat", "Chat with Doctor"),
        Service(4, "ai_chat", "Chat with Assistant"),
        Service(5, "drug_list", "Search for Drugs"),
        // Add more services as needed,
    )
}

fun doctorServices(): List<Service> {
    return listOf(
        Service(1, "m_appointment", "Your Appointments"),
        Service(2, "m_calendar", "Your Calendar"),
        Service(3, "m_prescription", "New Prescription"),
        Service(4, "m_patient_chats", "Chat with Patients"),

        // Add more services as needed,
    )
}