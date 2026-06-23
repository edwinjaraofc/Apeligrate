package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.UserProgressRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

class ProfileViewModel(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var profileJob: Job? = null

    fun loadUserProfile(userId: String) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userProgressRepository.ensureUser(userId)
            userProgressRepository.getUser(userId).collectLatest { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoading = false,
                        editName = user?.name.orEmpty(),
                        editCity = user?.city.orEmpty()
                    )
                }
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
            userProgressRepository.saveProfile(
                userId = user.id,
                name = currentState.editName,
                city = currentState.editCity
            )
            _uiState.update {
                it.copy(
                    user = user.copy(
                        name = currentState.editName,
                        city = currentState.editCity
                    ),
                    isLoading = false,
                    isEditing = false
                )
            }
        }
    }
}
