package com.apeligrate.domain.model

data class Contact(
    val id: String = "",
    val name: String,
    val phone: String,
    val isEmergency: Boolean = false
)
