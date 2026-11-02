package com.example.stockup.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.stockup.R
import com.example.stockup.data.ConsumoRepository
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
 * Responde ao "Acabei" / "Joguei fora" direto da notificação, sem abrir o app.
 *
 * É o coração do fluxo invertido: o usuário não navega até o produto para dizer que
 * ele acabou. Ele vê o aviso, toca em "Acabei" e pronto.
 *
 * Tocar em "Acabei" apaga um produto — então NUNCA pode ser irreversível. Por isso
 * este receiver imediatamente publica um aviso com "Desfazer", carregando o produto
 * dentro do próprio PendingIntent. A rede de proteção vai junto com a ação.
 */
class AcaoRapidaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val acao = intent.getStringExtra(EXTRA_ACAO) ?: return
        val app = context.applicationContext
        // goAsync dá alguns segundos para concluir a transação depois que o
        // onReceive retorna — sem isso o processo poderia morrer no meio.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (acao) {
                    ACAO_USEI -> concluir(app, intent, foiJogadoFora = false)
                    ACAO_JOGUEI_FORA -> concluir(app, intent, foiJogadoFora = true)
                    ACAO_DESFAZER -> desfazer(app, intent)
                }
            } catch (e: Exception) {
                // Falhar aqui não pode derrubar o app: o usuário só vê que nada mudou.
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun concluir(app: Context, intent: Intent, foiJogadoFora: Boolean) {
        val produtoId = intent.getLongExtra(EXTRA_PRODUTO_ID, -1L)
        if (produtoId <= 0L) return

        val banco = StockUpDatabase.getInstance(app)
        val produto = banco.productDao().getProductById(produtoId)

        // O produto pode já ter sido apagado no app. Nesse caso só limpa o aviso.
        if (produto == null) {
            NotificationManagerCompat.from(app).cancel(ExpirationWorker.NOTIFICACAO_ID)
            return
        }

        banco.productDao().deleteProduct(produto)
        val historicoId = banco.historyDao().insertRecord(
            ConsumptionHistory(
                productName = produto.name,
                quantity = produto.quantity,
                price = produto.price,
                wasWasted = foiJogadoFora,
                category = produto.category
            )
        )

        // "Acabei" também alimenta o motor de consumo — é daqui que sai a média
        // que estima quando o próximo vai acabar. "Joguei fora" não entra: descarte
        // não diz nada sobre o ritmo de consumo da casa.
        if (!foiJogadoFora) {
            registrarConsumo(banco, produto)
        }

        NotificationManagerCompat.from(app).cancel(ExpirationWorker.NOTIFICACAO_ID)
        confirmar(app, produto, historicoId, foiJogadoFora)
    }

    /** Anota quantidade consumida e há quantos dias o produto estava na despensa. */
    private suspend fun registrarConsumo(banco: StockUpDatabase, produto: Product) {
        val quantidade = CurrencyUtils.parseQuantity(produto.quantity, fallback = 0.0)
        if (quantidade <= 0.0) return
        val historico = ConsumoRepository(banco.consumoDao())
        val agora = System.currentTimeMillis()
        val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)
        val desde = historico.ultimaAtualizacao(chave).takeIf { it > 0L } ?: produto.createdAt
        val dias = ((agora - desde).toDouble() / DIA_MS).coerceAtLeast(0.5)
        historico.registrarConsumo(
            chave = chave,
            quantidade = quantidade,
            dias = dias,
            origem = "acabou",
            quando = agora
        )
    }

    /** Recria o produto e apaga o lançamento no histórico. */
    private suspend fun desfazer(app: Context, intent: Intent) {
        val json = intent.getStringExtra(EXTRA_PRODUTO_JSON) ?: return
        val produto = Gson().fromJson(json, Product::class.java) ?: return
        val historicoId = intent.getLongExtra(EXTRA_HISTORICO_ID, -1L)

        val banco = StockUpDatabase.getInstance(app)
        // insertProduct usa REPLACE, então volta com o mesmo id.
        banco.productDao().insertProduct(produto)
        if (historicoId > 0L) banco.historyDao().deleteRecordById(historicoId)

        NotificationManagerCompat.from(app).cancel(CONFIRMACAO_ID)
    }

    private fun confirmar(app: Context, produto: Product, historicoId: Long, foiJogadoFora: Boolean) {
        val desfazer = Intent(app, AcaoRapidaReceiver::class.java).apply {
            putExtra(EXTRA_ACAO, ACAO_DESFAZER)
            // O produto inteiro viaja dentro do PendingIntent: não há como perdê-lo
            // mesmo que o app seja encerrado entre o toque e o "Desfazer".
            putExtra(EXTRA_PRODUTO_JSON, Gson().toJson(produto))
            putExtra(EXTRA_HISTORICO_ID, historicoId)
        }
        val desfazerPendente = android.app.PendingIntent.getBroadcast(
            app,
            CONFIRMACAO_ID,
            desfazer,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val aviso = NotificationCompat.Builder(app, ExpirationWorker.CANAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${produto.name} saiu da lista")
            .setContentText(
                if (foiJogadoFora) "Anotado como jogado fora." else "Anotado como usado."
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "Desfazer", desfazerPendente)
            .build()

        try {
            NotificationManagerCompat.from(app).notify(CONFIRMACAO_ID, aviso)
        } catch (e: SecurityException) {
            // Sem permissão de notificação não há o que mostrar.
        }
    }

    companion object {
        const val EXTRA_ACAO = "acao"
        const val EXTRA_PRODUTO_ID = "produto_id"
        const val EXTRA_PRODUTO_JSON = "produto_json"
        const val EXTRA_HISTORICO_ID = "historico_id"

        const val ACAO_USEI = "usei"
        const val ACAO_JOGUEI_FORA = "joguei_fora"
        const val ACAO_DESFAZER = "desfazer"

        const val CONFIRMACAO_ID = 9001

        private const val DIA_MS = 24.0 * 60.0 * 60.0 * 1000.0
    }
}
