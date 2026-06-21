package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.Achievement
import com.apeligrate.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editCity: String = "",
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val mockUser = if (userId == "2") {
                User(
                    id = "2",
                    name = "María García",
                    email = "maria.garcia@example.com",
                    profileImageUrl = "https://randomuser.me/api/portraits/women/44.jpg",
                    city = "Guadalajara, MX",
                    isVerified = true,
                    level = 12,
                    experience = 1250,
                    nextLevelExperience = 2000,
                    reputationTitle = "Guardián de Élite",
                    reportsCount = 89,
                    validationsCount = 450,
                    achievements = listOf(
                        Achievement(
                            id = "4",
                            title = "Ojo de Halcón",
                            description = "Detectó 50 incidentes en menos de 1 hora.",
                            iconName = "visibility",
                            colorHex = "#4FC3F7"
                        ),
                        Achievement(
                            id = "5",
                            title = "Veterana",
                            description = "Más de 1 año protegiendo la comunidad.",
                            iconName = "star",
                            colorHex = "#FFD54F"
                        )
                    )
                )
            } else {
                User(
                    id = "1",
                    name = "Juan Pérez",
                    email = "juan.perez@example.com",
                    profileImageUrl = null,
                    city = "Ciudad de México, MX",
                    isVerified = true,
                    level = 8,
                    experience = 880,
                    nextLevelExperience = 1000,
                    reputationTitle = "Protector",
                    reportsCount = 45,
                    validationsCount = 120,
                    achievements = listOf(
                        Achievement(
                            id = "1",
                            title = "Vigilante",
                            description = "Por detectar 20 incidentes críticos en tiempo real.",
                            iconName = "visibility",
                            colorHex = "#FF5252"
                        ),
                        Achievement(
                            id = "2",
                            title = "Colaborador",
                            description = "Validó 100 reportes de la comunidad local.",
                            iconName = "diamond",
                            colorHex = "#FFC107"
                        )
                    )
                )
            }
            
            _uiState.update { 
                it.copy(
                    user = mockUser, 
                    isLoading = false,
                    editName = mockUser.name,
                    editCity = mockUser.city
                ) 
            }
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(editName = newName) }
    }

    fun onCityChange(newCity: String) {
        _uiState.update { it.copy(editCity = newCity) }
    }

    fun saveProfile() {
        val currentState = _uiState.value
        val user = currentState.user ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val updatedUser = user.copy(
                name = currentState.editName,
                city = currentState.editCity
            )
            
            _uiState.update { 
                it.copy(
                    user = updatedUser,
                    isLoading = false,
                    isEditing = false
                ) 
            }
        }
    }
}
