package com.example.stockup.ui.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.PantryLocation
import com.example.stockup.data.db.MARCADOR_DETALHES_PENDENTES
import com.example.stockup.data.db.Product
import com.example.stockup.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class AddProductUiState(
    val productId: Long? = null,
    val isEditMode: Boolean = false,
    val name: String = "",
    val quantity: String = "",
    val unit: String = "un",
    val expirationDate: Long? = null,
    val hasExpiration: Boolean = true,
    val photoUri: Uri? = null,
    val tempCameraUri: Uri? = null,
    val category: String = "Despensa",
    val location: String = "Geral",
    val priceDigits: String = "",
    val notes: String = "",
    val barcode: String = "",
    val isSearchingBarcode: Boolean = false,
    val barcodeMessage: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    val availableLocations: List<String> = listOf("Geral", "Geladeira", "Armário", "Despensa", "Freezer")
)

class AddProductViewModel(
    private val productId: Long? = null,
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductUiState(productId = productId, isEditMode = productId != null))
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    init {
        if (productId != null) {
            loadExistingProduct(productId)
        }
    }

    private fun loadExistingProduct(id: Long) {
        viewModelScope.launch {
            val product = repository.getProductById(id)
            if (product != null) {
                val digits = CurrencyUtils.doubleToCalculatorDigits(product.price)
                val hasExp = product.hasExpiration && product.expirationDate > 0L
                _uiState.value = _uiState.value.copy(
                    name = product.name,
                    quantity = product.quantity,
                    unit = product.unit,
                    expirationDate = if (hasExp) product.expirationDate else null,
                    hasExpiration = hasExp,
                    photoUri = product.photoUri?.let { Uri.parse(it) },
                    category = product.category,
                    location = product.location,
                    priceDigits = digits,
                    notes = product.notes,
                    barcode = product.barcode ?: ""
                )
            }
        }
    }

    fun updateName(name: String) {
        val cleanName = name.take(60)
        _uiState.value = _uiState.value.copy(name = cleanName, errorMessage = null)
    }

    fun updateQuantity(quantity: String) {
        _uiState.value = _uiState.value.copy(quantity = quantity, errorMessage = null)
    }

    fun updateUnit(unit: String) {
        _uiState.value = _uiState.value.copy(unit = unit)
    }

    fun toggleHasExpiration(hasExp: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasExpiration = hasExp,
            expirationDate = if (!hasExp) null else (_uiState.value.expirationDate ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)),
            errorMessage = null
        )
    }

    fun updateExpirationDate(timestamp: Long) {
        _uiState.value = _uiState.value.copy(expirationDate = timestamp, errorMessage = null)
    }

    fun setTempCameraUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(tempCameraUri = uri)
    }

    fun updatePhotoUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(photoUri = uri, tempCameraUri = null)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun updatePriceDigits(input: String) {
        val cleanDigits = input.filter { it.isDigit() }
        val safeDigits = if (cleanDigits.length > 10) cleanDigits.take(10) else cleanDigits
        _uiState.value = _uiState.value.copy(priceDigits = safeDigits, errorMessage = null)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun clearBarcode() {
        _uiState.value = _uiState.value.copy(barcode = "", barcodeMessage = null)
    }

    /**
     * Validador inteligente de nome do produto:
     * - Bloqueia nomes vazios ou com menos de 2 caracteres
     * - Limita em 60 caracteres
     * - Impede apenas números
     * - Impede repetições de caracteres excessivos (ex: aaaaa, 1111)
     * - Bloqueia digitação aleatória no teclado (5+ consoantes seguidas)
     * - Permite marcas legítimas com pontuações comerciais (ex: M&M's, WD-40, 7Up, H2OH!, Ypê, Omo)
     */
    private fun validateProductName(raw: String): String? {
        val name = raw.trim()
        if (name.isBlank()) return "Nome do produto é obrigatório"
        if (name.length < 2) return "Nome muito curto (mínimo 2 letras)"
        if (name.length > 60) return "Nome muito longo (máximo 60 caracteres)"
        if (name.all { it.isDigit() }) return "O nome não pode ser apenas números"

        // Bloqueia repetições excessivas (ex: "aaaa", "1111").
        // O \1 é backreference. O arquivo tinha um caractere de controle U+0001 no lugar
        // dele, o que fazia este Regex exigir bytes SOH e NUNCA casar nada (regra morta).
        val repeatRegex = Regex("""(.)\1{3,}""")
        if (repeatRegex.containsMatchIn(name)) {
            return "O nome tem muitas letras repetidas. Pode conferir?"
        }

        // Permite letras (com qualquer acento unicode), números, espaços e os símbolos
        // que aparecem de verdade em nomes de produto. O Open Food Facts devolve coisas
        // como "Nestlé® Classic" e "Molho de Tomate: Tradicional" — sem ® : ; * # @
        // o app reprovava um nome que ELE MESMO tinha acabado de preencher no scanner.
        val allowedRegex = Regex("""^[\p{L}\p{N}\s.\-&/%+'",()!:;*#@°ºª]+$""")
        if (!allowedRegex.matches(name)) {
            return "O nome tem um símbolo que não consigo usar. Pode tirar?"
        }

        // A regra que rejeitava 5+ consoantes seguidas foi removida: o allowedRegex
        // já limita os caracteres, e para este público um falso bloqueio ("seu produto
        // não pode ser salvo") é muito pior do que aceitar um nome esquisito.
        return null
    }

    /**
     * Processa a leitura do código de barras:
     * 1. Busca primeiro no banco de dados local do StockUp (instantâneo e offline)
     * 2. Se não achar, busca simultaneamente nas bases abertas mundiais (alimentos, limpeza, beleza)
     * 3. Preenche nome, marca, foto e categoria automaticamente
     */
    fun onBarcodeScanned(scannedCode: String) {
        val cleanCode = scannedCode.trim()
        if (cleanCode.isBlank()) return

        // O leitor já não aceita QR Code, mas validamos de novo aqui: o valor vira
        // coluna do banco e é interpolado na URL do Open Food Facts. Código de produto
        // real (EAN-8/13, UPC-A/E, Code-128 numérico) tem de 8 a 14 dígitos.
        if (!cleanCode.matches(Regex("^\\d{8,14}$"))) {
            _uiState.value = _uiState.value.copy(
                barcode = "",
                isSearchingBarcode = false,
                barcodeMessage = "Esse código não parece um código de produto."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            barcode = cleanCode,
            isSearchingBarcode = true,
            barcodeMessage = "Buscando dados do produto..."
        )

        viewModelScope.launch {
            // 1. Tenta buscar no banco de dados local primeiro
            val localMatch = repository.getProductByBarcode(cleanCode)
            if (localMatch != null) {
                val digits = CurrencyUtils.doubleToCalculatorDigits(localMatch.price)
                // Preserva a foto que o usuário tirou; se não tiver, usa a do banco
                val preservedPhoto = _uiState.value.photoUri ?: localMatch.photoUri?.let { Uri.parse(it) }
                _uiState.value = _uiState.value.copy(
                    name = localMatch.name.take(60),
                    category = if (_uiState.value.category == "Despensa") localMatch.category else _uiState.value.category,
                    unit = localMatch.unit,
                    priceDigits = if (digits.isNotBlank()) digits else _uiState.value.priceDigits,
                    photoUri = preservedPhoto,
                    isSearchingBarcode = false,
                    barcodeMessage = "Produto memorizado encontrado: ${localMatch.name}"
                )
                return@launch
            }

            // 2. Busca nas bases públicas abertas (Open Food Facts v2/v0, Open Products e Open Beauty)
            val onlineResult = withContext(Dispatchers.IO) {
                fetchProductOnline(cleanCode)
            }

            if (onlineResult != null) {
                val (resolvedName, matchedCat, imageUrl) = onlineResult
                // take(60): o validador limita a 60 caracteres, e nomes do Open Food Facts
                // costumam ser longos. Sem isso o app reprovava um nome que ele mesmo preencheu.
                val finalName = (resolvedName ?: _uiState.value.name).take(60)
                // Preserva a foto que o usuário tirou; se não tiver foto própria, usa a foto da internet
                val preservedPhoto = _uiState.value.photoUri ?: imageUrl?.let { Uri.parse(it) }
                val preservedCategory = if (_uiState.value.category == "Despensa") (matchedCat ?: _uiState.value.category) else _uiState.value.category
                _uiState.value = _uiState.value.copy(
                    name = finalName,
                    category = preservedCategory,
                    photoUri = preservedPhoto,
                    isSearchingBarcode = false,
                    barcodeMessage = "Produto encontrado: $finalName"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSearchingBarcode = false,
                    barcodeMessage = "Código lido! Digite o nome uma vez para memorizar."
                )
            }
        }
    }

    private suspend fun fetchProductOnline(barcode: String): Triple<String?, String?, String?>? = withContext(Dispatchers.IO) {
        // URLs otimizadas (alimentos mundial/Brasil, produtos gerais/limpeza, e higiene/beleza)
        val endpoints = listOf(
            "https://world.openfoodfacts.org/api/v2/product/$barcode?fields=product_name,product_name_pt,product_name_br,generic_name,generic_name_pt,brands,categories,image_url,image_front_url",
            "https://br.openfoodfacts.org/api/v0/product/$barcode.json",
            "https://world.openproductsfacts.org/api/v0/product/$barcode.json",
            "https://world.openbeautyfacts.org/api/v0/product/$barcode.json"
        )

        // Consulta os endpoints em paralelo com timeout rápido de 3.5 segundos para não prender o usuário
        val tasks = endpoints.map { urlStr ->
            async {
                try {
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 3500
                    conn.readTimeout = 3500
                    conn.setRequestProperty("User-Agent", "StockUpApp - Android - Version 1.0 (davi@stockup.com)")

                    if (conn.responseCode == 200) {
                        val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(jsonStr)
                        val status = root.optInt("status", -1)
                        if (status == 1 || root.optString("status_verbose") == "product found") {
                            val product = root.optJSONObject("product")
                            if (product != null) {
                                val rawName = product.optString("product_name_pt").ifBlank {
                                    product.optString("product_name_br").ifBlank {
                                        product.optString("product_name").ifBlank {
                                            product.optString("generic_name_pt").ifBlank {
                                                product.optString("generic_name")
                                            }
                                        }
                                    }
                                }
                                val brand = product.optString("brands", "").trim()
                                val imageUrl = product.optString("image_front_url").ifBlank {
                                    product.optString("image_url").ifBlank { null }
                                }
                                val categories = product.optString("categories", "").lowercase()

                                val finalName = when {
                                    rawName.isNotBlank() && brand.isNotBlank() && !rawName.contains(brand, ignoreCase = true) -> "$rawName ($brand)"
                                    rawName.isNotBlank() -> rawName
                                    brand.isNotBlank() -> brand
                                    else -> null
                                }

                                val matchedCategory = when {
                                    "limpeza" in categories || "detergente" in categories || "cleaning" in categories || "sabão" in categories -> "Limpeza"
                                    "higiene" in categories || "sabonete" in categories || "shampoo" in categories || "pasta de dente" in categories -> "Higiene"
                                    "leite" in categories || "queijo" in categories || "dairy" in categories || "iogurte" in categories -> "Laticínios"
                                    "bebida" in categories || "beverage" in categories || "suco" in categories || "refrigerante" in categories || "cerveja" in categories -> "Bebidas"
                                    "carne" in categories || "meat" in categories || "frango" in categories || "peixe" in categories -> "Carnes"
                                    else -> null
                                }

                                if (finalName != null || imageUrl != null) {
                                    return@async Triple(finalName, matchedCategory, imageUrl)
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
                null
            }
        }

        // Retorna o primeiro resultado válido encontrado
        val results = tasks.awaitAll()
        results.firstOrNull { it != null }
    }

    fun saveProduct() {
        val state = _uiState.value

        // Validação inteligente do nome
        val nameValidation = validateProductName(state.name)
        if (nameValidation != null) {
            _uiState.value = state.copy(errorMessage = nameValidation)
            return
        }

        if (state.quantity.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Quantidade é obrigatória")
            return
        }
        // Aceita vírgula (teclado pt-BR) além do ponto. fallback 0.0 mantém a
        // rejeição de quantidade em branco/inválida logo abaixo.
        val qtyVal = CurrencyUtils.parseQuantity(state.quantity, fallback = 0.0)
        if (qtyVal <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Informe uma quantidade válida (ex: 1, 500, 2.5)")
            return
        }
        if (state.hasExpiration) {
            val exp = state.expirationDate
            if (exp != null) {
                // NÃO bloqueamos datas passadas de propósito: registrar um produto que já
                // venceu é legítimo — é assim que o usuário registra o desperdício, e o app
                // existe justamente para isso. Só barramos o absurdo (o DatePicker do M3
                // aceita de 1900 a 2100), que só pode ser toque errado.
                val year = Calendar.getInstance().apply { timeInMillis = exp }.get(Calendar.YEAR)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                if (year < currentYear - 2 || year > currentYear + 20) {
                    _uiState.value = state.copy(
                        errorMessage = "Confira a validade: o ano $year parece engano"
                    )
                    return
                }
            }
            // Sem data informada NÃO é erro: a validade é opcional. O produto é
            // salvo sem validade (o app não inventa data) e passa a ser acompanhado
            // pela quantidade — o motor de consumo usa o padrão da categoria.
        }
        val priceVal = CurrencyUtils.parseCalculatorPriceToDouble(state.priceDigits)
        if (priceVal <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Informe o preço do produto")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val product = Product(
                id = productId ?: 0L,
                name = state.name.trim(),
                quantity = state.quantity.trim(),
                unit = state.unit.ifBlank { "un" },
                // "Perecível" SEM data = validade desconhecida. Grava sem validade
                // (hasExpiration = false) em vez de inventar uma data: a Carta não
                // admite número sem explicação, e data inventada vira alarme falso.
                // O produto não fica perdido — o motor de consumo o acompanha pela
                // quantidade e pelo padrão da categoria.
                expirationDate = if (state.hasExpiration && state.expirationDate != null) {
                    state.expirationDate
                } else {
                    0L
                },
                hasExpiration = state.hasExpiration && state.expirationDate != null,
                // A coluna continua no banco (sem migração), mas o valor agora é
                // sempre 0: o "estoque mínimo" deixou de existir como conceito.
                minStock = 0.0,
                photoUri = state.photoUri?.toString(),
                category = state.category.trim(),
                location = state.location.trim().ifBlank { "Geral" },
                price = priceVal,
                notes = limparMarcadorPendente(state.notes.trim(), state.hasExpiration, state.expirationDate),
                barcode = state.barcode.trim().ifBlank { null }
            )

            if (productId != null && productId > 0) {
                repository.updateProduct(product)
            } else {
                repository.insertProduct(product)
            }

            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }

    /**
     * O "Mover Rápido" de Compras cria o produto com um marcador dentro das anotações.
     * Ele nunca era removido, então o selo "Detalhes pendentes" continuava aparecendo
     * mesmo depois de o usuário preencher validade e local. Ao salvar, se a validade
     * já foi resolvida (data informada, ou marcado como não perecível), o marcador sai.
     */
    private fun limparMarcadorPendente(
        anotacoes: String,
        hasExpiration: Boolean,
        expirationDate: Long?
    ): String {
        if (!anotacoes.contains(MARCADOR_DETALHES_PENDENTES, ignoreCase = true)) return anotacoes
        val detalhesResolvidos = !hasExpiration || (expirationDate ?: 0L) > 0L
        if (!detalhesResolvidos) return anotacoes
        return anotacoes
            .replace(MARCADOR_DETALHES_PENDENTES, "", ignoreCase = true)
            .replace("⚠️", "")
            .trim()
    }

    fun addQuickLocation(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                repository.insertLocation(PantryLocation(name = trimmed))
                _uiState.value = _uiState.value.copy(location = trimmed)
            }
        }
    }
}
