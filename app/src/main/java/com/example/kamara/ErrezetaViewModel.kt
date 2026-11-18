package com.example.kamara

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Jetpack ViewModel: UI eta Firebase repository-aren arteko zubia. */
class ErrezetaViewModel : ViewModel() {
    private val repo = ErrezetaRepository()

    private val _errezetak = MutableStateFlow<List<Errezeta>>(emptyList())
    val errezetak: StateFlow<List<Errezeta>> = _errezetak.asStateFlow()

    private val _kargatzen = MutableStateFlow(false)
    val kargatzen: StateFlow<Boolean> = _kargatzen.asStateFlow()

    private val _errorea = MutableStateFlow<String?>(null)
    val errorea: StateFlow<String?> = _errorea.asStateFlow()

    fun kargatu() {
        viewModelScope.launch {
            _kargatzen.value = true
            _errorea.value = null
            _errezetak.value = runCatching { repo.lortuGuztiak() }
                .onFailure { _errorea.value = it.message }
                .getOrDefault(emptyList())
            _kargatzen.value = false
        }
    }

    fun gorde(errezeta: Errezeta, argazkiaUri: Uri?, onAmaituta: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.gorde(errezeta, argazkiaUri) }
                .onFailure { _errorea.value = it.message }
                .onSuccess { onAmaituta() }
        }
    }

    fun ezabatu(errezeta: Errezeta) {
        viewModelScope.launch {
            runCatching { repo.ezabatu(errezeta) }
                .onFailure { _errorea.value = it.message }
            kargatu()
        }
    }

    fun erroreaGarbitu() { _errorea.value = null }
}
