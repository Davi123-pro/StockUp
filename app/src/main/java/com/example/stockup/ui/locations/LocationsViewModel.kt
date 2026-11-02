package com.example.stockup.ui.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.PantryLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationsUiState(
    val locations: List<PantryLocation> = emptyList(),
    val newLocationName: String = "",
    val errorMessage: String? = null
)

class LocationsViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _newLocationName = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LocationsUiState> = combine(
        repository.allLocations,
        _newLocationName,
        _errorMessage
    ) { locations, name, error ->
        LocationsUiState(
            locations = locations,
            newLocationName = name,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocationsUiState())

    val updateNewLocationName: (String) -> Unit = { name ->
        _newLocationName.value = name
        _errorMessage.value = null
    }

    val insertLocation: () -> Unit = {
        val name = _newLocationName.value.trim()
        if (name.isBlank()) {
            _errorMessage.value = "O nome do local não pode ser vazio"
        } else {
            viewModelScope.launch {
                repository.insertLocation(PantryLocation(name = name))
                _newLocationName.value = ""
            }
        }
    }

    val deleteLocation: (PantryLocation) -> Unit = { location ->
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }
}
