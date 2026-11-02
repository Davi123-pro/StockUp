package com.example.stockup.data.backup

import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.data.db.PantryLocation
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.ShoppingItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Testes de `BackupManager` (gravar/ler o .zip de cópia de segurança).
 *
 * Regressões cobertas: cópia que perde campos ao restaurar (id, código de barras,
 * preço nulo, "foi jogado fora"), arquivo de outro app aceito como cópia, versão de
 * formato futura lida como se fosse atual, zip inválido derrubando o app com erro
 * feio, acentos/emoji corrompidos e entrada duplicada de foto estourando o zip.
 *
 * IMPORTANTE (ambiente): todos os `Product` daqui usam `photoUri = null` (ou uma URL
 * https, que é ignorada) justamente para o teste não depender do `android.net.Uri`,
 * que é um stub que lança RuntimeException("Stub!") fora do Android.
 *
 * ATENÇÃO (bloqueio conhecido): `BackupManager` usa `org.json`, que no android.jar do
 * SDK também é um stub que lança RuntimeException("Stub!") em teste JVM
 * (verificado com o android-36). Sem uma implementação REAL de `org.json` no
 * classpath de teste estes casos falham, e `returnDefaultValues = true` NÃO resolve
 * (devolveria null/0 e a leitura recusaria a cópia). O ajuste necessário é de build,
 * fora de `app/src/test/`: ex. `testImplementation("org.json:json:20250107")`.
 */
class BackupManagerTest {

    @get:Rule
    val pastaTemporaria = TemporaryFolder()

    // ---------------------------------------------------------------- round-trip

    @Test
    fun copiaEEscritaELidaPreservandoTodosOsCampos() = runTest {
        // Regressão: restaurar a cópia precisa devolver as quatro tabelas idênticas
        // (campo a campo, ids inclusive) — qualquer campo esquecido no JSON vira
        // dado perdido silenciosamente na restauração.
        val fotosDir = pastaTemporaria.newFolder("fotos")

        val produtosOriginais = listOf(
            produto(
                id = 1L,
                nome = "Arroz Tipo 1",
                photoUri = null,
                quantity = "5",
                unit = "kg",
                expirationDate = 1_800_000_000_000L,
                category = "Despensa",
                notes = "Prateleira de cima",
                price = 24.9,
                location = "Armário",
                createdAt = 1_700_000_000_000L,
                hasExpiration = true,
                minStock = 2.0,
                barcode = null
            ),
            produto(
                id = 2L,
                nome = "Leite Integral",
                photoUri = null,
                quantity = "2",
                unit = "L",
                expirationDate = 1_810_000_000_000L,
                category = "Bebidas",
                notes = "Comprar 2 caixas",
                price = 5.49,
                location = "Geladeira",
                createdAt = 1_700_000_100_000L,
                hasExpiration = false,
                minStock = 1.5,
                barcode = "7891000100103"
            )
        )

        val comprasOriginais = listOf(
            itemDeCompra(
                id = 10L,
                nome = "Café",
                quantity = "1",
                unit = "un",
                category = "Despensa",
                price = null,
                isCompleted = false,
                isUrgent = true,
                createdAt = 1_700_000_200_000L
            ),
            itemDeCompra(
                id = 11L,
                nome = "Sabão em pó",
                quantity = "3",
                unit = "un",
                category = "Limpeza",
                price = 4.5,
                isCompleted = true,
                isUrgent = false,
                createdAt = 1_700_000_300_000L
            )
        )

        val historicoOriginal = listOf(
            consumo(
                id = 20L,
                productName = "Arroz Tipo 1",
                quantity = "1",
                price = 4.98,
                wasWasted = false,
                category = "Despensa",
                timestamp = 1_700_000_400_000L
            ),
            consumo(
                id = 21L,
                productName = "Leite Integral",
                quantity = "2",
                price = 10.98,
                wasWasted = true,
                category = "Bebidas",
                timestamp = 1_700_000_500_000L
            )
        )

        val locaisOriginais = listOf(
            localDaDespensa(id = 30L, nome = "Geladeira"),
            localDaDespensa(id = 31L, nome = "Armário")
        )

        val saida = ByteArrayOutputStream()
        val resumo = BackupManager.write(
            out = saida,
            products = produtosOriginais,
            shopping = comprasOriginais,
            history = historicoOriginal,
            locations = locaisOriginais
        )

        assertEquals(2, resumo.products)
        assertEquals(2, resumo.shopping)
        assertEquals(2, resumo.history)
        assertEquals(2, resumo.locations)
        assertEquals(0, resumo.photos)

        val conteudo = BackupManager.read(ByteArrayInputStream(saida.toByteArray()), fotosDir)

        assertEquals(produtosOriginais, conteudo.products)
        assertEquals(comprasOriginais, conteudo.shopping)
        assertEquals(historicoOriginal, conteudo.history)
        assertEquals(locaisOriginais, conteudo.locations)
        assertEquals(0, conteudo.photos)
    }

    // ---------------------------------------------------------------- backup vazio

    @Test
    fun copiaVaziaGravaELeSemErro() = runTest {
        // Regressão: despensa zerada precisa gerar uma cópia válida (antes, um zip
        // sem nenhuma tabela podia ser tratado como arquivo corrompido).
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val saida = ByteArrayOutputStream()

        val resumo = BackupManager.write(saida, emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(0, resumo.products)
        assertEquals(0, resumo.shopping)
        assertEquals(0, resumo.history)
        assertEquals(0, resumo.locations)
        assertEquals(0, resumo.photos)

        val conteudo = BackupManager.read(ByteArrayInputStream(saida.toByteArray()), fotosDir)
        assertTrue(conteudo.products.isEmpty())
        assertTrue(conteudo.shopping.isEmpty())
        assertTrue(conteudo.history.isEmpty())
        assertTrue(conteudo.locations.isEmpty())
        assertEquals(0, conteudo.photos)
    }

    // ---------------------------------------------------------------- validações

    @Test
    fun copiaDeOutroAplicativoEhRejeitada() = runTest {
        // Regressão: um zip qualquer (de outro app) não pode ser importado como se
        // fosse cópia do StockUp e sobrescrever o banco.
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val bytes = zipComJson("""{"app":"OutroApp","formatVersion":1}""")

        val erro = erroAoLer(bytes, fotosDir)

        assertTrue("esperava IllegalArgumentException, veio: $erro", erro is IllegalArgumentException)
    }

    @Test
    fun copiaDeVersaoMaisNovaEhRejeitada() = runTest {
        // Regressão: cópia feita por uma versão futura tem campos que este código não
        // entende; ler mesmo assim perderia dados. Tem de ser recusada.
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val bytes = zipComJson("""{"app":"StockUp","formatVersion":99}""")

        val erro = erroAoLer(bytes, fotosDir)

        assertTrue("esperava IllegalArgumentException, veio: $erro", erro is IllegalArgumentException)
    }

    @Test
    fun copiaComVersaoZeroEhRejeitada() = runTest {
        // Regressão: sem versão válida (0 ou ausente) o formato é desconhecido e não
        // pode ser lido às cegas.
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val bytes = zipComJson("""{"app":"StockUp","formatVersion":0}""")

        val erro = erroAoLer(bytes, fotosDir)

        assertTrue("esperava IllegalArgumentException, veio: $erro", erro is IllegalArgumentException)
    }

    @Test
    fun arquivoQueNaoEhZipEhRejeitado() = runTest {
        // Regressão: escolher um .txt/.jpg no seletor de arquivos não pode estourar um
        // erro técnico na cara do usuário; o arquivo tem de ser recusado com a
        // mensagem de "não parece uma cópia".
        // Obs.: ZipInputStream.readLOC devolve null quando a assinatura do header é
        // inválida (inclusive entrada curta demais), então não escapa ZipException —
        // o fluxo acaba em `dados == null` e cai na IllegalArgumentException.
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val bytes = "isto não é um zip".toByteArray(Charsets.UTF_8)

        val erro = erroAoLer(bytes, fotosDir)

        assertTrue("esperava IllegalArgumentException, veio: $erro", erro is IllegalArgumentException)
    }

    @Test
    fun copiaSemAsTabelasEhAceitaComListasVazias() = runTest {
        // Regressão: cópia de uma versão antiga/simplificada, sem as chaves das
        // tabelas, precisa ser tolerada (listas vazias) em vez de quebrar a leitura.
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val bytes = zipComJson("""{"app":"StockUp","formatVersion":1}""")

        val conteudo = BackupManager.read(ByteArrayInputStream(bytes), fotosDir)

        assertTrue(conteudo.products.isEmpty())
        assertTrue(conteudo.shopping.isEmpty())
        assertTrue(conteudo.history.isEmpty())
        assertTrue(conteudo.locations.isEmpty())
        assertEquals(0, conteudo.photos)
    }

    // ---------------------------------------------------------------- unicode

    @Test
    fun acentosEEmojiSobrevivemACopia() = runTest {
        // Regressão: o JSON era lido/escrito em charset errado e "Feijão"/"Ação"
        // voltavam corrompidos (ou o emoji derrubava a leitura).
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val nome = "Feijão Carioca 1kg — Ação"
        val observacoes = "Comprado na promoção 🛒 2ª unidade"

        val originais = listOf(
            produto(id = 7L, nome = nome, photoUri = null, notes = observacoes, barcode = null)
        )

        val saida = ByteArrayOutputStream()
        BackupManager.write(saida, originais, emptyList(), emptyList(), emptyList())
        val conteudo = BackupManager.read(ByteArrayInputStream(saida.toByteArray()), fotosDir)

        assertEquals(1, conteudo.products.size)
        assertEquals(nome, conteudo.products[0].name)
        assertEquals(observacoes, conteudo.products[0].notes)
        assertEquals(originais, conteudo.products)
    }

    // ---------------------------------------------------------------- fotos

    @Test
    fun doisProdutosComAMesmaFotoNaoQuebramAGravacao() = runTest {
        // Regressão: dois produtos apontando para a mesma imagem geravam duas
        // entradas com o mesmo nome no zip -> ZipException ("duplicate entry") e a
        // cópia inteira falhava. URL da internet é ignorada (não é arquivo local).
        val fotosDir = pastaTemporaria.newFolder("fotos")
        val uriExterna = "https://exemplo.com/x.jpg"
        val produtos = listOf(
            produto(id = 1L, nome = "Produto A", photoUri = uriExterna, barcode = null),
            produto(id = 2L, nome = "Produto B", photoUri = uriExterna, barcode = null)
        )

        val saida = ByteArrayOutputStream()
        val resumo = BackupManager.write(saida, produtos, emptyList(), emptyList(), emptyList())

        assertEquals(2, resumo.products)
        assertEquals(0, resumo.photos)

        val conteudo = BackupManager.read(ByteArrayInputStream(saida.toByteArray()), fotosDir)
        assertEquals(0, conteudo.photos)
        assertEquals(uriExterna, conteudo.products[0].photoUri)
        assertEquals(uriExterna, conteudo.products[1].photoUri)
    }
}

// ---------------------------------------------------------------- helpers

/**
 * Lê [bytes] e devolve o erro lançado, ou null se a leitura passou.
 *
 * Ajuda os testes de rejeição a mostrar o erro real na mensagem de falha.
 */
private suspend fun erroAoLer(bytes: ByteArray, fotosDir: File): Throwable? = try {
    BackupManager.read(ByteArrayInputStream(bytes), fotosDir)
    null
} catch (erro: Throwable) {
    erro
}

/** Monta um zip com uma única entrada `dados.json` contendo [json] (UTF-8). */
private fun zipComJson(json: String): ByteArray {
    val saida = ByteArrayOutputStream()
    ZipOutputStream(saida).use { zip ->
        zip.putNextEntry(ZipEntry("dados.json"))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    return saida.toByteArray()
}

private fun produto(
    id: Long,
    nome: String = "Arroz Tipo 1",
    photoUri: String? = null,
    quantity: String = "5",
    unit: String = "kg",
    expirationDate: Long = 1_800_000_000_000L,
    category: String = "Despensa",
    notes: String = "Prateleira de cima",
    price: Double = 24.9,
    location: String = "Armário",
    createdAt: Long = 1_700_000_000_000L,
    hasExpiration: Boolean = true,
    minStock: Double = 2.0,
    barcode: String? = null
): Product = Product(
    id = id,
    name = nome,
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

private fun itemDeCompra(
    id: Long,
    nome: String,
    quantity: String = "1",
    unit: String = "un",
    category: String = "Despensa",
    price: Double? = null,
    isCompleted: Boolean = false,
    isUrgent: Boolean = false,
    createdAt: Long = 1_700_000_000_000L
): ShoppingItem = ShoppingItem(
    id = id,
    name = nome,
    quantity = quantity,
    unit = unit,
    category = category,
    price = price,
    isCompleted = isCompleted,
    isUrgent = isUrgent,
    createdAt = createdAt
)

private fun consumo(
    id: Long,
    productName: String,
    quantity: String,
    price: Double,
    wasWasted: Boolean,
    category: String = "Despensa",
    timestamp: Long = 1_700_000_000_000L
): ConsumptionHistory = ConsumptionHistory(
    id = id,
    productName = productName,
    quantity = quantity,
    price = price,
    wasWasted = wasWasted,
    category = category,
    timestamp = timestamp
)

private fun localDaDespensa(id: Long, nome: String): PantryLocation =
    PantryLocation(id = id, name = nome)
