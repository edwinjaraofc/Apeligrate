package com.apeligrate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apeligrate.domain.model.AuthCredentials
import com.apeligrate.domain.use_case.RegisterUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegisterSuccess: Boolean = false
)

class RegisterViewModel(
    private val registerUseCase: RegisterUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onEmailChange(email: String) { _uiState.value = _uiState.value.copy(email = email) }
    fun onPasswordChange(password: String) { _uiState.value = _uiState.value.copy(password = password) }
    fun onConfirmPasswordChange(password: String) { _uiState.value = _uiState.value.copy(confirmPassword = password) }
    fun onTermsToggle(accepted: Boolean) { _uiState.value = _uiState.value.copy(termsAccepted = accepted) }

    fun register() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Completa todos los campos")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }
        if (!state.termsAccepted) {
            _uiState.value = state.copy(error = "Debes aceptar los términos")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = registerUseCase(state.name, AuthCredentials(state.email, state.password))
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isRegisterSuccess = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
