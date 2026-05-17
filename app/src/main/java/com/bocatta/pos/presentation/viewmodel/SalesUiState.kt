package com.bocatta.pos.presentation.viewmodel

data class SalesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSuccess: Boolean = false
)

