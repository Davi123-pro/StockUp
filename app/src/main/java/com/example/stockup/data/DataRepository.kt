package com.example.stockup.data

import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.data.db.HistoryDao
import com.example.stockup.data.db.LocationDao
import com.example.stockup.data.db.MARCADOR_EXEMPLO_TOUR
import com.example.stockup.data.db.PantryLocation
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.ProductDao
import com.example.stockup.data.db.ShoppingDao
import com.example.stockup.data.db.ShoppingItem
import com.example.stockup.utils.CurrencyUtils
import com.example.stockup.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val shoppingDao: ShoppingDao,
    private val historyDao: HistoryDao,
    private val locationDao: LocationDao
) {

    // --- Produtos ---
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): Product? =
        productDao.getProductById(id)

    fun getProductFlow(id: Long): Flow<Product?> =
        productDao.getProductFlow(id)

    suspend fun getProductByBarcode(barcode: String): Product? =
        productDao.getProductByBarcode(barcode)

    suspend fun insertProduct(product: Product): Long =
        productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) =
        productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) =
        productDao.deleteProduct(product)

    suspend fun deleteProductById(id: Long) =
        productDao.deleteProductById(id)

    suspend fun getProductsExpiringSoon(thresholdTimestamp: Long, floorTimestamp: Long): List<Product> =
        productDao.getProductsExpiringSoon(thresholdTimestamp, floorTimestamp)

    // --- Restauração de cópia de segurança ---
    // Apaga tudo antes de regravar: uma cópia é um retrato completo da despensa,
    // não um complemento. Sem isso, restaurar duas vezes duplicaria os produtos.

    suspend fun deleteAllProducts() = productDao.deleteAllProducts()

    // --- Produto de exemplo (existe só durante o tour) ---

    /**
     * Cria o produto falso que o tour usa para ter o que apontar.
     *
     * Quem abre o app pela primeira vez não tem nada na despensa, e o passo que
     * ensina o "Acabou" ficaria apontando para o vazio.
     */
    suspend fun criarProdutoDeExemplo(photoUri: String?) {
        productDao.deleteByMarcador(MARCADOR_EXEMPLO_TOUR)
        productDao.insertProduct(
            Product(
                // Sem "(exemplo)" no nome: para quem está aprendendo, o produto é
                // só "Leite". O sufixo poluía o nome e não ensinava nada.
                name = "Leite",
                quantity = "1",
                unit = "L",
                // Vence em 2 dias: aparece como "Vence em 2 dias", sem assustar.
                expirationDate = DateUtils.daysFromNow(2),
                hasExpiration = true,
                minStock = 0.0,
                photoUri = photoUri,
                category = "Bebidas",
                location = "Geladeira",
                price = 5.49,
                notes = MARCADOR_EXEMPLO_TOUR
            )
        )
    }

    suspend fun removerProdutoDeExemplo() =
        productDao.deleteByMarcador(MARCADOR_EXEMPLO_TOUR)

    suspend fun deleteAllShoppingItems() = shoppingDao.deleteAllItems()

    suspend fun deleteAllLocations() = locationDao.deleteAllLocations()

    // --- Lista de Compras ---
    val allShoppingItems: Flow<List<ShoppingItem>> = shoppingDao.getAllItems()

    suspend fun insertShoppingItem(item: ShoppingItem): Long =
        shoppingDao.insertItem(item)

    /**
     * Põe o produto na lista — ou SOMA na quantidade se ele já estiver lá.
     *
     * Antes, tocar duas vezes em "Comprar mais" criava duas linhas iguais na lista.
     * A comparação ignora maiúsculas, acentos e espaços extras: "Leite Cemil",
     * "leite cemil" e "LEITE  CEMIL" são o mesmo produto. Nomes genuinamente
     * diferentes ("Leite Cemil" e "Leite Ninho") continuam separados.
     */
    suspend fun adicionarNaListaSomando(
        name: String,
        unit: String,
        category: String,
        price: Double
    ): Int {
        val chave = normalizarNome(name)
        val existente = shoppingDao.getAllItemsOnce().firstOrNull {
            !it.isCompleted && normalizarNome(it.name) == chave
        }

        if (existente == null) {
            shoppingDao.insertItem(
                ShoppingItem(
                    name = name.trim(),
                    quantity = "1",
                    unit = unit.ifBlank { "un" },
                    category = category,
                    price = price
                )
            )
            return 1
        }

        val atual = CurrencyUtils.parseQuantity(existente.quantity, fallback = 1.0)
        val novo = atual + 1.0
        shoppingDao.updateItem(
            existente.copy(
                quantity = if (novo % 1.0 == 0.0) novo.toInt().toString() else novo.toString().replace('.', ',')
            )
        )
        return novo.toInt()
    }

    /** Minúsculas, sem acento e sem espaço dobrado. */
    private fun normalizarNome(texto: String): String =
        java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")

    /**
     * Tira 1 da quantidade de um item da lista — o contrário de [adicionarNaListaSomando].
     *
     * Existe para o "Desfazer" da pergunta do dia: quando o app põe o produto na
     * lista sozinho, ele precisa saber tirar exatamente aquele 1 de volta, sem
     * apagar um item que a pessoa já tinha anotado antes.
     */
    suspend fun removerDaListaSomando(name: String): Boolean {
        val chave = normalizarNome(name)
        val existente = shoppingDao.getAllItemsOnce().firstOrNull {
            !it.isCompleted && normalizarNome(it.name) == chave
        } ?: return false

        val atual = CurrencyUtils.parseQuantity(existente.quantity, fallback = 1.0)
        return if (atual > 1.0) {
            shoppingDao.updateItem(
                existente.copy(
                    quantity = CurrencyUtils.formatarQuantidade(atual - 1.0)
                )
            )
            true
        } else {
            shoppingDao.deleteItem(existente)
            true
        }
    }

    suspend fun updateShoppingItem(item: ShoppingItem) =
        shoppingDao.updateItem(item)

    suspend fun deleteShoppingItem(item: ShoppingItem) =
        shoppingDao.deleteItem(item)

    suspend fun deleteCompletedShoppingItems() =
        shoppingDao.deleteCompletedItems()

    // --- Histórico de Consumo ---
    val allHistory: Flow<List<ConsumptionHistory>> = historyDao.getAllHistory()

    fun getHistorySince(startTimestamp: Long): Flow<List<ConsumptionHistory>> =
        historyDao.getHistorySince(startTimestamp)

    suspend fun insertConsumptionRecord(record: ConsumptionHistory): Long =
        historyDao.insertRecord(record)

    /** Usado pelo "Desfazer" logo após apagar um produto. */
    suspend fun deleteConsumptionRecordById(id: Long) =
        historyDao.deleteRecordById(id)

    /** Corrige para "jogado fora" um consumo que tinha sido registrado como usado. */
    suspend fun marcarConsumoComoDesperdicio(ids: List<Long>) =
        historyDao.marcarComoDesperdicio(ids)

    suspend fun clearHistory() =
        historyDao.clearHistory()

    // --- Locais ---
    val allLocations: Flow<List<PantryLocation>> = locationDao.getAllLocations()

    suspend fun insertLocation(location: PantryLocation): Long =
        locationDao.insertLocation(location)

    suspend fun deleteLocation(location: PantryLocation) =
        locationDao.deleteLocation(location)
}
