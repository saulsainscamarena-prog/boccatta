package com.bocatta.pos.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.ThemeRepository
import com.bocatta.pos.domain.model.ThemeConfigV2
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val repository: ThemeRepository
) : BaseViewModel() {

    private val _config = MutableStateFlow(ThemeConfigV2())
    val config: StateFlow<ThemeConfigV2> = _config.asStateFlow()
    private var observeJob: Job? = null

    init {
        iniciarObservacionSiAutenticado()
    }

    fun iniciarObservacionSiAutenticado() {
        if (observeJob != null || FirebaseAuth.getInstance().currentUser == null) return
        observeJob = viewModelScope.launch(safeHandler) {
            repository.observarTema().collect { _config.value = it }
        }
    }

    fun guardar(
        primaryHex: String,
        secondaryHex: String,
        tertiaryHex: String,
        updatedBy: String
    ) {
        val p = normalizeHex(primaryHex)
        val s = normalizeHex(secondaryHex)
        val t = normalizeHex(tertiaryHex)
        if (p == null || s == null || t == null) {
            mensajeError = "Usa colores validos en formato #RRGGBB"
            return
        }
        viewModelScope.launch(safeHandler) {
            cargando = true
            repository.guardar(
                ThemeConfigV2(
                    enabled = true,
                    primaryHex = p,
                    secondaryHex = s,
                    tertiaryHex = t,
                    updatedBy = updatedBy
                )
            )
            cargando = false
            mensajeExito = "Apariencia actualizada"
        }
    }

    fun restaurar() {
        viewModelScope.launch(safeHandler) {
            cargando = true
            repository.restaurar()
            cargando = false
            mensajeExito = "Apariencia Bocatta restaurada"
        }
    }

    private fun normalizeHex(input: String): String? {
        val clean = input.trim().removePrefix("#")
        if (!Regex("^[0-9a-fA-F]{6}$").matches(clean)) return null
        return "#${clean.uppercase()}"
    }
}
