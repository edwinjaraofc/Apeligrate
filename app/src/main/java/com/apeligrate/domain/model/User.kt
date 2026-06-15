package com.apeligrate.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val city: String = "Ciudad de México, MX",
    val isVerified: Boolean = false,
    
    // Gamification
    val level: Int = 1,
    val experience: Int = 0,
    val nextLevelExperience: Int = 1000,
    val reputationTitle: String = "Iniciante", // e.g., Protector, Guardián, Héroe
    val reportsCount: Int = 0,
    val validationsCount: Int = 0,
    val achievements: List<Achievement> = emptyList()
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String, // To map to an icon
    val colorHex: String,
    val isUnlocked: Boolean = false
)
