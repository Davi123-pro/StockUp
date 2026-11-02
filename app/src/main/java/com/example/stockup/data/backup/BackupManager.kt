package com.example.stockup.data.backup

import android.net.Uri
import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.data.db.PantryLocation
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.ShoppingItem
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** O que foi gravado numa cópia, para a mensagem de confirmação. */
data class BackupSummary(
    val products: Int,
    val shopping: Int,
    val history: Int,
    val locations: Int,
    val photos: Int
)

/** O que foi lido de uma cópia, pronto para gravar no banco. */
data class BackupContent(
    val products: List<Product>,
    val shopping: List<ShoppingItem>,
    val history: List<ConsumptionHistory>,
    val locations: List<PantryLocation>,
    val photos: Int
)

/**
 * Salva e lê uma cópia de segurança da despensa.
 *
 * Formato: um arquivo `.zip` com `dados.json` (as quatro tabelas) e a pasta `fotos/`.
 *
 * As fotos vão junto de propósito. Uma cópia que salva só os textos parece
 * funcionar e depois mostra a despensa inteira sem imagem — o usuário acha que
 * perdeu o trabalho. Com elas, a restauração num celular novo fica igual ao antigo.
 *
 * Usa kotlinx-serialization em vez de `org.json`: além de type-safe, `org.json` é um
 * STUB nos testes JVM do AGP (o `android.jar` só tem o esqueleto), o que impediria
 * testar o formato sem um celular na mão.
 */
object BackupManager {

    const val MIME_TYPE = "application/zip"
    const val SUGGESTED_FILE_NAME = "stockup-copia"

    /** Nome esperado do diretório de fotos dentro de `filesDir` (ver ImageFileUtils). */
    const val PHOTOS_DIR_NAME = "product_images"

    private const val ENTRY_DATA = "dados.json"
    private const val DIR_PHOTOS = "fotos/"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // ---------------------------------------------------------------- gravar

    suspend fun write(
        out: OutputStream,
        products: List<Product>,
        shopping: List<ShoppingItem>,
        history: List<ConsumptionHistory>,
        locations: List<PantryLocation>
    ): BackupSummary = withContext(Dispatchers.IO) {
        val jaGravadas = mutableSetOf<String>()
        var photos = 0

        ZipOutputStream(out.buffered()).use { zip ->
            val dados = BackupDto(
                savedAt = System.currentTimeMillis(),
                products = products.map { it.paraDto() },
                shoppingItems = shopping.map { it.paraDto() },
                history = history.map { it.paraDto() },
                locations = locations.map { it.paraDto() }
            )

            zip.putNextEntry(ZipEntry(ENTRY_DATA))
            zip.write(gson.toJson(dados).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            products.forEach { product ->
                val nome = photoFileName(product.photoUri) ?: return@forEach
                // Dois produtos podem apontar para a mesma foto: entrada duplicada
                // no zip estoura ZipException, então gravamos uma vez só.
                if (!jaGravadas.add(nome)) return@forEach
                val arquivo = localPhotoFile(product.photoUri) ?: return@forEach

                zip.putNextEntry(ZipEntry("$DIR_PHOTOS$nome"))
                arquivo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                photos++
            }
        }

        BackupSummary(products.size, shopping.size, history.size, locations.size, photos)
    }

    // ---------------------------------------------------------------- ler

    /**
     * @param photosDir pasta onde as fotos serão reescritas (normalmente
     *        `File(context.filesDir, "product_images")`).
     */
    suspend fun read(input: InputStream, photosDir: File): BackupContent = withContext(Dispatchers.IO) {
        var dados: String? = null
        val fotosRestauradas = mutableMapOf<String, String>()
        var photos = 0

        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == ENTRY_DATA -> dados = zip.readBytes().toString(Charsets.UTF_8)

                    entry.name.startsWith(DIR_PHOTOS) && !entry.isDirectory -> {
                        val nome = entry.name.removePrefix(DIR_PHOTOS)
                        // Ignora subpastas e nomes vazios vindos de um zip malformado.
                        if (nome.isNotBlank() && !nome.contains('/') && !nome.contains('\\')) {
                            if (!photosDir.exists()) photosDir.mkdirs()
                            val destino = File(photosDir, nome)
                            destino.outputStream().use { zip.copyTo(it) }
                            fotosRestauradas[nome] = Uri.fromFile(destino).toString()
                            photos++
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val texto = dados ?: throw IllegalArgumentException("Este arquivo não parece uma cópia do StockUp.")

        val dto = runCatching {
            gson.fromJson(texto, BackupDto::class.java)
        }.getOrElse { erro ->
            if (erro is JsonSyntaxException) {
                throw IllegalArgumentException("Esta cópia está danificada e não pôde ser lida.")
            }
            throw erro
        } ?: throw IllegalArgumentException("Esta cópia está vazia.")

        if (dto.app != APP_TAG) {
            throw IllegalArgumentException("Este arquivo não é uma cópia do StockUp.")
        }
        if (dto.formatVersion <= 0 || dto.formatVersion > FORMAT_VERSION) {
            throw IllegalArgumentException(
                "Esta cópia foi feita por uma versão mais nova do aplicativo."
            )
        }

        BackupContent(
            products = dto.products.map { it.paraEntidade(fotosRestauradas) },
            shopping = dto.shoppingItems.map { it.paraEntidade() },
            history = dto.history.map { it.paraEntidade() },
            locations = dto.locations.map { it.paraEntidade() },
            photos = photos
        )
    }

    // ---------------------------------------------------------------- helpers

    /** Nome do arquivo de uma foto local, ou null se for URL da internet / vazio. */
    fun photoFileName(photoUri: String?): String? {
        if (photoUri.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(photoUri) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null
        val nome = uri.lastPathSegment ?: return null
        return nome.takeIf { it.isNotBlank() }
    }

    private fun localPhotoFile(photoUri: String?): File? {
        if (photoUri.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(photoUri) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null
        val caminho = uri.path ?: return null
        val arquivo = File(caminho)
        return arquivo.takeIf { it.exists() && it.isFile }
    }

    // ---------------------------------------------------------------- mapeamento

    private fun Product.paraDto() = ProductDto(
        id = id,
        name = name,
        photoUri = photoUri,
        quantity = quantity,
        unit = unit,
        expirationDate = expirationDate,
        category = category,
        notes = notes,
        price = price,
        location = location,
        createdAt = createdAt,
        hasExpiration = hasExpiration,
        minStock = minStock,
        barcode = barcode
    )

    private fun ProductDto.paraEntidade(fotos: Map<String, String>) = Product(
        id = id,
        name = name,
        // Se a foto veio no zip, aponta para o arquivo recém-extraído.
        photoUri = photoUri?.let { antigo -> photoFileName(antigo)?.let { fotos[it] } ?: antigo },
        quantity = quantity,
        unit = unit,
        expirationDate = expirationDate,
        category = category,
        notes = notes,
        price = price,
        location = location,
        createdAt = createdAt,
        hasExpiration = hasExpiration,
        minStock = minStock,
        barcode = barcode
    )

    private fun ShoppingItem.paraDto() = ShoppingItemDto(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        price = price,
        isCompleted = isCompleted,
        isUrgent = isUrgent,
        createdAt = createdAt
    )

    private fun ShoppingItemDto.paraEntidade() = ShoppingItem(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        price = price,
        isCompleted = isCompleted,
        isUrgent = isUrgent,
        createdAt = createdAt
    )

    private fun ConsumptionHistory.paraDto() = HistoryDto(
        id = id,
        productName = productName,
        quantity = quantity,
        price = price,
        wasWasted = wasWasted,
        category = category,
        timestamp = timestamp
    )

    private fun HistoryDto.paraEntidade() = ConsumptionHistory(
        id = id,
        productName = productName,
        quantity = quantity,
        price = price,
        wasWasted = wasWasted,
        category = category,
        timestamp = timestamp
    )

    private fun PantryLocation.paraDto() = LocationDto(id = id, name = name)

    private fun LocationDto.paraEntidade() = PantryLocation(id = id, name = name)
}

// ---------------------------------------------------------------- formato do arquivo

private const val APP_TAG = "StockUp"
private const val FORMAT_VERSION = 1

internal data class BackupDto(
    val app: String = APP_TAG,
    val formatVersion: Int = FORMAT_VERSION,
    val savedAt: Long = 0L,
    val products: List<ProductDto> = emptyList(),
    val shoppingItems: List<ShoppingItemDto> = emptyList(),
    val history: List<HistoryDto> = emptyList(),
    val locations: List<LocationDto> = emptyList()
)

internal data class ProductDto(
    val id: Long = 0L,
    val name: String = "",
    val photoUri: String? = null,
    val quantity: String = "1",
    val unit: String = "un",
    val expirationDate: Long = 0L,
    val category: String = "Despensa",
    val notes: String = "",
    val price: Double = 0.0,
    val location: String = "Geral",
    val createdAt: Long = 0L,
    val hasExpiration: Boolean = true,
    val minStock: Double = 0.0,
    val barcode: String? = null
)

internal data class ShoppingItemDto(
    val id: Long = 0L,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "un",
    val category: String = "Despensa",
    val price: Double? = null,
    val isCompleted: Boolean = false,
    val isUrgent: Boolean = false,
    val createdAt: Long = 0L
)

internal data class HistoryDto(
    val id: Long = 0L,
    val productName: String = "",
    val quantity: String = "1",
    val price: Double = 0.0,
    val wasWasted: Boolean = false,
    val category: String = "Despensa",
    val timestamp: Long = 0L
)

internal data class LocationDto(
    val id: Long = 0L,
    val name: String = ""
)
