package com.example.stockup.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.stockup.R
import com.example.stockup.data.ConsumoRepository
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.StockUpDatabase
import com.example.stockup.domain.ConsumptionEngine
import com.example.stockup.utils.CurrencyUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Responde ao aviso de consumo sem abrir o app.
 *
 * As três respostas que cabem na notificação (o Android só mostra três botões):
 *
 * - **Ainda tenho** — o app errou a previsão: o produto durou mais. Empurra o
 *   próximo aviso e deixa a pessoa em paz.
 * - **Está acabando** — põe na lista de compras (é o que ela ia fazer de qualquer
 *   forma) e volta a perguntar em dois dias.
 * - **Já acabou** — registra o consumo (é daqui que sai a média), tira da
 *   despensa e põe na lista de compras.
 *
 * "Atualizar quantidade" e "Responder mais tarde" não cabem como botão: tocar no
 * corpo do aviso abre o produto (onde dá para corrigir a quantidade) e, se ninguém
 * responde, o aviso volta no intervalo calculado — que dobra a cada silêncio.
 *
 * Tudo o que muda a despensa publica um "Desfazer" logo depois.
 */
class AvisoConsumoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val acao = intent.getStringExtra(EXTRA_ACAO) ?: return
        val app = context.applicationContext
        val pendente = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val banco = StockUpDatabase.getInstance(app)
                val repositorio = ProductRepository(
                    banco.productDao(),
                    banco.shoppingDao(),
                    banco.historyDao(),
                    banco.locationDao()
                )
                when (acao) {
                    ACAO_AINDA_TENHO -> aindaTenho(app, banco, intent)
                    ACAO_ACABANDO -> estaAcabando(app, banco, repositorio, intent)
                    ACAO_ACABOU -> jaAcabou(app, banco, repositorio, intent)
                    ACAO_DESFAZER -> desfazer(app, banco, repositorio, intent)
                }
            } catch (e: Exception) {
                // Falhar aqui não pode derrubar nada.
            } finally {
                pendente.finish()
            }
        }
    }

    /** "Ainda tenho": a previsão estava adiantada. Volta a perguntar em 2 dias. */
    private suspend fun aindaTenho(app: Context, banco: StockUpDatabase, intent: Intent) {
        val produto = buscarProduto(app, banco, intent) ?: return
        val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)

        cancelarAviso(app)
        ConsumoRepository(banco.consumoDao()).registrarResposta(
            chave = chave,
            resposta = ConsumoRepository.RESPOSTA_AINDA_TENHO,
            proximo = System.currentTimeMillis() + 2 * DIA_MS
        )
        confirmar(
            app = app,
            produto = produto,
            titulo = "Combinado: ${produto.name} ainda está aí",
            texto = "Volto a perguntar em 2 dias.",
            desfazer = null,
            entrouNaLista = false
        )
    }

    /** "Está acabando": entra na lista de compras e volta a perguntar em 2 dias. */
    private suspend fun estaAcabando(
        app: Context,
        banco: StockUpDatabase,
        repositorio: ProductRepository,
        intent: Intent
    ) {
        val produto = buscarProduto(app, banco, intent) ?: return
        val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)

        cancelarAviso(app)
        repositorio.adicionarNaListaSomando(
            name = produto.name,
            unit = produto.unit,
            category = produto.category,
            price = produto.price
        )
        ConsumoRepository(banco.consumoDao()).registrarResposta(
            chave = chave,
            resposta = ConsumoRepository.RESPOSTA_ACABANDO,
            proximo = System.currentTimeMillis() + 2 * DIA_MS
        )
        confirmar(
            app = app,
            produto = produto,
            titulo = "${produto.name} está na lista de compras",
            texto = "Já anotei para você comprar.",
            desfazer = ACAO_DESFAZER,
            entrouNaLista = true
        )
    }

    /** "Já acabou": registra o consumo, tira da despensa e põe na lista. */
    private suspend fun jaAcabou(
        app: Context,
        banco: StockUpDatabase,
        repositorio: ProductRepository,
        intent: Intent
    ) {
        val produto = buscarProduto(app, banco, intent) ?: return
        val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)
        val quantidade = CurrencyUtils.parseQuantity(produto.quantity, fallback = 1.0)

        cancelarAviso(app)

        val agora = System.currentTimeMillis()
        val historico = ConsumoRepository(banco.consumoDao())
        val desde = historico.ultimaAtualizacao(chave).takeIf { it > 0L } ?: produto.createdAt
        val dias = ((agora - desde).toDouble() / DIA_MS).coerceAtLeast(0.5)
        historico.registrarConsumo(
            chave = chave,
            quantidade = quantidade,
            dias = dias,
            origem = "aviso",
            quando = agora
        )
        historico.registrarResposta(
            chave = chave,
            resposta = ConsumoRepository.RESPOSTA_ACABOU,
            proximo = 0L
        )

        banco.productDao().deleteProduct(produto)
        val historicoId = banco.historyDao().insertRecord(
            ConsumptionHistory(
                productName = produto.name,
                quantity = produto.quantity,
                price = produto.price,
                wasWasted = false,
                category = produto.category
            )
        )
        val entrouNaLista = runCatching {
            repositorio.adicionarNaListaSomando(
                name = produto.name,
                unit = produto.unit,
                category = produto.category,
                price = produto.price
            )
        }.isSuccess

        confirmar(
            app = app,
            produto = produto,
            titulo = "${produto.name} saiu da despensa",
            texto = if (entrouNaLista) {
                "Anotado como usado e já está na lista de compras."
            } else {
                "Anotado como usado."
            },
            desfazer = ACAO_DESFAZER,
            entrouNaLista = entrouNaLista,
            historicoId = historicoId
        )
    }

    /** Volta tudo: produto, histórico, item da lista e a observação de consumo. */
    private suspend fun desfazer(
        app: Context,
        banco: StockUpDatabase,
        repositorio: ProductRepository,
        intent: Intent
    ) {
        val json = intent.getStringExtra(EXTRA_PRODUTO_JSON) ?: return
        val produto = Gson().fromJson(json, Product::class.java) ?: return
        val historicoId = intent.getLongExtra(EXTRA_HISTORICO_ID, -1L)
        val entrouNaLista = intent.getBooleanExtra(EXTRA_ENTROU_NA_LISTA, false)
        val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)

        banco.productDao().insertProduct(produto)
        if (historicoId > 0L) banco.historyDao().deleteRecordById(historicoId)
        if (entrouNaLista) repositorio.removerDaListaSomando(produto.name)

        val historico = ConsumoRepository(banco.consumoDao())
        historico.removerUltimaObservacao(chave)
        historico.registrarResposta(chave, resposta = "", proximo = 0L)

        NotificationManagerCompat.from(app).cancel(CONFIRMACAO_ID)
    }

    private suspend fun buscarProduto(app: Context, banco: StockUpDatabase, intent: Intent): Product? {
        val id = intent.getLongExtra(EXTRA_PRODUTO_ID, -1L)
        val produto = if (id > 0L) banco.productDao().getProductById(id) else null
        if (produto == null) cancelarAviso(app)
        return produto
    }

    private fun cancelarAviso(app: Context) {
        NotificationManagerCompat.from(app).cancel(AvisoDeConsumo.AVISO_ID)
    }

    private fun confirmar(
        app: Context,
        produto: Product,
        titulo: String,
        texto: String,
        desfazer: String?,
        entrouNaLista: Boolean,
        historicoId: Long = 0L
    ) {
        val construtor = NotificationCompat.Builder(app, AvisoDeConsumo.CANAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (desfazer != null) {
            val intento = Intent(app, AvisoConsumoReceiver::class.java).apply {
                putExtra(EXTRA_ACAO, ACAO_DESFAZER)
                putExtra(EXTRA_PRODUTO_JSON, Gson().toJson(produto))
                putExtra(EXTRA_HISTORICO_ID, historicoId)
                putExtra(EXTRA_ENTROU_NA_LISTA, entrouNaLista)
            }
            construtor.addAction(
                0,
                "Desfazer",
                PendingIntent.getBroadcast(
                    app,
                    CONFIRMACAO_ID,
                    intento,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        try {
            NotificationManagerCompat.from(app).notify(CONFIRMACAO_ID, construtor.build())
        } catch (e: SecurityException) {
            // Sem permissão, sem aviso.
        }
    }

    companion object {
        const val EXTRA_ACAO = "acao"
        const val EXTRA_PRODUTO_ID = "produto_id"
        const val EXTRA_PRODUTO_JSON = "produto_json"
        const val EXTRA_HISTORICO_ID = "historico_id"
        const val EXTRA_ENTROU_NA_LISTA = "entrou_na_lista"

        const val ACAO_AINDA_TENHO = "ainda_tenho"
        const val ACAO_ACABANDO = "acabando"
        const val ACAO_ACABOU = "acabou"
        const val ACAO_DESFAZER = "desfazer"

        /** Id da confirmação. Separado do aviso (3001) de propósito. */
        const val CONFIRMACAO_ID = 3002

        private const val DIA_MS = 24L * 60L * 60L * 1000L
    }
}
