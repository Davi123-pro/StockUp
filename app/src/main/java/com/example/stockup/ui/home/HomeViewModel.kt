package com.example.stockup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ConsumoRepository
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.data.db.Product
import com.example.stockup.domain.BuscaDeProduto
import com.example.stockup.domain.ConsumptionEngine
import com.example.stockup.utils.CurrencyUtils
import com.example.stockup.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductFilter {
    ALL, EXPIRING_SOON, EXPIRED
}

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val filter: ProductFilter = ProductFilter.ALL,
    val selectedLocation: String = "Todos",
    val locations: List<String> = listOf("Todos"),
    val searchQuery: String = "",
    val hasMultipleLocations: Boolean = false
)

class HomeViewModel(
    private val repository: ProductRepository,
    private val consumoRepository: ConsumoRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ProductFilter.ALL)
    private val _selectedLocation = MutableStateFlow("Todos")
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allProducts,
        _filter,
        _selectedLocation,
        _searchQuery
    ) { products, filter, location, query ->
        // Extrai locais unicos cadastrados nos produtos (removendo vazios)
        val distinctUserLocations = products
            .map { it.location.trim() }
            .filter { it.isNotBlank() && !it.equals("Geral", ignoreCase = true) }
            .distinct()
            .sorted()

        val hasMultiple = distinctUserLocations.isNotEmpty()
        val extractedLocations = if (hasMultiple) {
            listOf("Todos") + (listOf("Geral") + distinctUserLocations).distinct()
        } else {
            listOf("Todos")
        }

        val filtered = products
            .filter { product ->
                // Busca sem acento, sem caixa e sem espaço sobrando — e também no
                // local e na categoria, que é como a pessoa lembra do produto
                // ("armário" acha o azeite mesmo sem ela lembrar o nome).
                BuscaDeProduto.combina(
                    nome = product.name,
                    local = product.location,
                    categoria = product.category,
                    consulta = query
                )
            }
            .filter { product ->
                location == "Todos" || product.location.equals(location, ignoreCase = true)
            }
            .filter { product ->
                when (filter) {
                    ProductFilter.ALL -> true
                    ProductFilter.EXPIRING_SOON -> {
                        val days = DateUtils.daysUntilExpiration(product.expirationDate)
                        days in 0..7
                    }
                    ProductFilter.EXPIRED -> {
                        DateUtils.daysUntilExpiration(product.expirationDate) < 0
                    }
                }
            }

        HomeUiState(
            products = filtered,
            filter = filter,
            selectedLocation = location,
            locations = extractedLocations,
            searchQuery = query,
            hasMultipleLocations = hasMultiple
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setFilter(filter: ProductFilter) {
        _filter.value = filter
    }

    fun setLocation(location: String) {
        _selectedLocation.value = location
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ─────────────── Exclusão com "Desfazer" ───────────────
    // A Carta diz que o app não pode dar medo. Por isso toda exclusão guarda o que
    // apagou, para o usuário poder voltar atrás com um toque.

    private var ultimaExclusao: ExclusaoDesfeita? = null

    private val _exclusao = MutableStateFlow<EventoExclusao?>(null)
    val exclusao: StateFlow<EventoExclusao?> = _exclusao.asStateFlow()

    fun limparEventoExclusao() {
        _exclusao.value = null
    }

    /** Ao excluir um item unitário. */
    fun deleteProduct(product: Product, wasWasted: Boolean) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            val id = repository.insertConsumptionRecord(
                ConsumptionHistory(
                    productName = product.name,
                    quantity = product.quantity,
                    price = product.price,
                    wasWasted = wasWasted,
                    category = product.category
                )
            )
            // "Usei" também alimenta o motor de consumo: quantidade e há quantos
            // dias o produto estava na despensa. É daqui que sai a média que
            // estima quando o próximo vai acabar. Desperdício não entra — jogar
            // fora não diz nada sobre o ritmo de consumo da casa.
            if (!wasWasted) registrarConsumoDe(product)
            ultimaExclusao = ExclusaoDesfeita(listOf(product), listOf(id))
            _exclusao.value = EventoExclusao(
                mensagem = "\"${product.name}\" saiu da lista.",
                podeMarcarDesperdicio = true
            )
        }
    }

    fun deleteProductJustRemove(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            ultimaExclusao = ExclusaoDesfeita(listOf(product), emptyList())
            _exclusao.value = EventoExclusao("\"${product.name}\" foi apagado.")
        }
    }

    /** Exclusão em lote. */
    fun deleteMultipleProducts(products: List<Product>, recordHistory: Boolean, wasWasted: Boolean = false) {
        viewModelScope.launch {
            val ids = mutableListOf<Long>()
            products.forEach { p ->
                repository.deleteProduct(p)
                if (recordHistory) {
                    ids += repository.insertConsumptionRecord(
                        ConsumptionHistory(
                            productName = p.name,
                            quantity = p.quantity,
                            price = p.price,
                            wasWasted = wasWasted,
                            category = p.category
                        )
                    )
                    if (!wasWasted) registrarConsumoDe(p)
                }
            }
            ultimaExclusao = ExclusaoDesfeita(products, ids)
            _exclusao.value = EventoExclusao(
                mensagem = if (products.size == 1) {
                    "1 produto saiu da lista."
                } else {
                    "${products.size} produtos saíram da lista."
                },
                podeMarcarDesperdicio = recordHistory
            )
        }
    }

    /**
     * Anota o consumo no histórico que alimenta a estimativa.
     *
     * "Dias transcorridos" = da última mudança de estoque (ou do cadastro) até
     * agora. Se o produto ficou meses parado, a média sai baixa — e o próprio
     * histórico corrige isso na segunda vez.
     */
    private suspend fun registrarConsumoDe(produto: Product) {
        val quantidade = CurrencyUtils.parseQuantity(produto.quantity, fallback = 0.0)
        if (quantidade <= 0.0) return
        val agora = System.currentTimeMillis()
        val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)
        val desde = consumoRepository.ultimaAtualizacao(chave).takeIf { it > 0L } ?: produto.createdAt
        val dias = ((agora - desde).toDouble() / DIA_MS).coerceAtLeast(0.5)
        consumoRepository.registrarConsumo(
            chave = chave,
            quantidade = quantidade,
            dias = dias,
            origem = "acabou",
            quando = agora
        )
    }

    /** Repõe tudo o que a última exclusão removeu, e desfaz o lançamento no histórico. */
    fun desfazerExclusao() {
        val lote = ultimaExclusao ?: return
        ultimaExclusao = null
        viewModelScope.launch {
            // insertProduct usa REPLACE, então o produto volta com o mesmo id.
            lote.produtos.forEach { repository.insertProduct(it) }
            lote.historicoIds.forEach { repository.deleteConsumptionRecordById(it) }
            // O "Desfazer" também desfaz o que o motor aprendeu: registrar um
            // consumo que não aconteceu envenenaria todas as previsões seguintes.
            lote.produtos.forEach { produto ->
                consumoRepository.removerUltimaObservacao(
                    ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)
                )
            }
            // Confirmação visível. Sem isso a barra sumia na hora do toque e o
            // usuário ficava sem saber se voltou — e o produto reaparece na posição
            // dele na lista, que pode estar fora da tela.
            _exclusao.value = EventoExclusao("Pronto, voltou!", podeDesfazer = false)
        }
    }
    /** Corrige o último registro de "usei" para "joguei fora". */
    fun marcarUltimaExclusaoComoDesperdicio() {
        val lote = ultimaExclusao ?: return
        if (lote.historicoIds.isEmpty()) return
        viewModelScope.launch {
            repository.marcarConsumoComoDesperdicio(lote.historicoIds)
            // A exclusão continua guardada: o "Desfazer" segue valendo.
            _exclusao.value = EventoExclusao("Marcado como jogado fora.")
        }
    }
}

/** O que a última exclusão apagou, para poder voltar atrás. */
private data class ExclusaoDesfeita(val produtos: List<Product>, val historicoIds: List<Long>)

private const val DIA_MS = 24.0 * 60.0 * 60.0 * 1000.0

/**
 * Aviso mostrado depois de apagar.
 *
 * O [podeMarcarDesperdicio] liga o segundo atalho da barra ("Foi jogado fora").
 * Existe porque o caminho principal assumiu "usei" sem perguntar, e o usuário
 * precisa poder corrigir isso sem refazer tudo.
 */
data class EventoExclusao(
    val mensagem: String,
    val podeMarcarDesperdicio: Boolean = false,
    /** Falso quando já não há o que desfazer (ex.: logo depois de desfazer). */
    val podeDesfazer: Boolean = true
)
