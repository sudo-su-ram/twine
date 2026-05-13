package com.tether.app.ui.screens

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    fun login(twinCode: String) {
        // TODO: Implement actual login logic with repository
        // For now, just simulate success
        _loginState.value = LoginState.Loading
        
        // Simulate network delay
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(1000)
            _loginState.value = LoginState.Success
        }
    }
}
