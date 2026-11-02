package com.example.stockup.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.data.db.Product
import com.example.stockup.utils.CurrencyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class ProductDetailViewModel(
    private val productId: Long,
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        // Observa o banco em vez de tirar um retrato único. Antes, editar a validade
        // (ou qualquer outro campo) só aparecia aqui depois de sair do produto e entrar
        // de novo, porque o estado guardava para sempre o objeto lido no init.
        viewModelScope.launch {
            repository.getProductFlow(productId).collect { product ->
                // Após excluir, o Flow emite null — não sobrescrevemos a saída da tela.
                if (_uiState.value.isDeleted) return@collect
                _uiState.value = if (product != null) {
                    _uiState.value.copy(product = product, isLoading = false, errorMessage = null)
                } else {
                    _uiState.value.copy(isLoading = false, errorMessage = "Produto não encontrado")
                }
            }
        }
    }

    fun deleteProduct(wasWasted: Boolean) = viewModelScope.launch {
        val currentProduct = _uiState.value.product ?: return@launch
        // Marca antes de apagar: o Flow vai emitir null e não deve sobrescrever a saída.
        _uiState.value = _uiState.value.copy(isDeleted = true)
        repository.deleteProduct(currentProduct)
        
        // Registra no histórico de consumo/desperdício
        repository.insertConsumptionRecord(
            ConsumptionHistory(
                productName = currentProduct.name,
                quantity = currentProduct.quantity,
                price = currentProduct.price,
                wasWasted = wasWasted,
                category = currentProduct.category
            )
        )
        
    }

    /**
     * Passo do +/- conforme a unidade. Somar 1 numa Coca de 1,5 L daria 2,5 L
     * (sem sentido) e somar 0,5 em ovo também não. Ver VISAO_PRODUTO.md.
     */
    private fun stepFor(unit: String): Double = when (unit.lowercase().trim()) {
        "l", "kg" -> 0.5
        "g", "ml" -> 50.0
        else -> 1.0  // un, cx, pc e qualquer outra
    }

    /** Grava "0,5" (vírgula, como o brasileiro lê) e inteiro sem casas decimais. */
    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else value.toString().replace(".", ",")

    fun incrementQuantity() = viewModelScope.launch {
        val currentProduct = _uiState.value.product ?: return@launch
        val newQty = CurrencyUtils.parseQuantity(currentProduct.quantity) + stepFor(currentProduct.unit)
        val updated = currentProduct.copy(quantity = formatQuantity(newQty))
        repository.updateProduct(updated)
        _uiState.value = _uiState.value.copy(product = updated)
    }

    fun decrementQuantity() = viewModelScope.launch {
        val currentProduct = _uiState.value.product ?: return@launch
        val step = stepFor(currentProduct.unit)
        val qty = CurrencyUtils.parseQuantity(currentProduct.quantity)
        // Não desce abaixo de um passo: chegar a zero é o fluxo de excluir/"acabou".
        if (qty - step < step) return@launch
        val updated = currentProduct.copy(quantity = formatQuantity(qty - step))
        repository.updateProduct(updated)
        _uiState.value = _uiState.value.copy(product = updated)
    }
}
