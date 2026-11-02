package com.example.stockup.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.MARCADOR_DETALHES_PENDENTES
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.ShoppingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val items: List<ShoppingItem> = emptyList(),
    val quickInputText: String = "",
    val itemsToMove: List<ShoppingItem> = emptyList()
)

class ShoppingViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _quickInputText = MutableStateFlow("")
    val quickInputText = _quickInputText.asStateFlow()

    private val _itemsToMove = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val itemsToMove = _itemsToMove.asStateFlow()

    val uiState: StateFlow<ShoppingUiState> = repository.allShoppingItems
        .map { items ->
            ShoppingUiState(items = items)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingUiState())

    fun updateQuickInput(text: String) {
        _quickInputText.value = text
    }

    fun addItem() {
        val text = _quickInputText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.insertShoppingItem(
                ShoppingItem(
                    name = text,
                    quantity = "1",
                    unit = "un"
                )
            )
            _quickInputText.value = ""
        }
    }

    fun addItemWithDetails(name: String, quantity: String, unit: String, category: String, price: Double?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertShoppingItem(
                ShoppingItem(
                    name = name.trim(),
                    quantity = if (quantity.isBlank()) "1" else quantity.trim(),
                    unit = unit,
                    category = category,
                    price = price
                )
            )
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.updateShoppingItem(item)
        }
    }

    fun toggleItemCompleted(item: ShoppingItem) {
        viewModelScope.launch {
            repository.updateShoppingItem(
                item.copy(isCompleted = !item.isCompleted)
            )
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun prepareSelectedItemsToMove() {
        val completed = uiState.value.items.filter { it.isCompleted }
        _itemsToMove.value = completed
    }

    fun clearItemsToMove() {
        _itemsToMove.value = emptyList()
    }

    fun moveCheckedItemsQuicklyToPantry() {
        viewModelScope.launch {
            val completed = uiState.value.items.filter { it.isCompleted }
            completed.forEach { item ->
                val product = Product(
                    name = item.name.trim(),
                    quantity = if (item.quantity.isNotBlank()) item.quantity.trim() else "1",
                    unit = if (item.unit.isNotBlank()) item.unit.trim() else "un",
                    price = item.price ?: 0.0,
                    category = if (item.category.isNotBlank()) item.category.trim() else "Despensa",
                    location = "Geral",
                    hasExpiration = false,
                    expirationDate = 0L,
                    minStock = 0.0,
                    notes = MARCADOR_DETALHES_PENDENTES
                )
                repository.insertProduct(product)
            }
            repository.deleteCompletedShoppingItems()
            clearItemsToMove()
        }
    }

    fun moveItemsToPantry(products: List<Product>) {
        viewModelScope.launch {
            products.forEach { product ->
                repository.insertProduct(product)
            }
            repository.deleteCompletedShoppingItems()
            clearItemsToMove()
        }
    }
}
