package com.example.e_clinic.Services

data class Service(
    val id: Int,
    val name: String,
    val displayedName: String,
    val description: String  = ""
)

//TODO: add description, icon
