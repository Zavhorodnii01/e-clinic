package com.example.e_clinic.Services.functions

import com.example.e_clinic.Services.Service

fun appServices(): List<Service> {
    return listOf(
        Service(1, "My Appointments", "My Appointments"),
        Service(2, "My Prescriptions", "My Prescriptions"),
        Service(3, "Chat with Doctor", "Chat with Doctor"),
        Service(4, "Chat with AI Assistant", "Chat with AI Assistant"),
        //Service(5, "Search for Drugs", "Search for Drugs"),
        // Add more services as needed,
    )
}

fun doctorServices(): List<Service> {
    return listOf(
        Service(1, "Appointments", "Appointments"),
        //Service(2, "m_calendar", "Your Calendar"),
        Service(3, "New Prescription", "New Prescription"),
        Service(4, "Chat with Patients", "Chat with Patients"),

        // Add more services as needed,
    )
}