package com.example.stockup.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockup.MainActivity
import com.example.stockup.R
import com.example.stockup.data.ConsumoRepository
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.StockUpDatabase
import com.example.stockup.domain.ConsumptionEngine
import com.example.stockup.domain.HorarioDoDia
import com.example.stockup.domain.OrigemEstimativa
import com.example.stockup.utils.CurrencyUtils
import com.example.stockup.utils.UserPreferences
import kotlinx.coroutines.flow.first

/**
 * Uma vez por dia, olha a despensa e decide se há algo para avisar.
 *
 * O que entra na conta:
 * - produtos **sem validade** (os com validade já têm o aviso de vencimento);
 * - quantidade maior que zero;
 * - previsão de término a menos de um dia (ou já passada);
 * - produto não avisado nos últimos dias (intervalo que dobra a cada silêncio);
 * - nada comprado nas últimas 24 h (senão o aviso chega junto com a compra).
 *
 * No máximo UM aviso por dia, e nunca no mesmo dia do aviso de validade — a Carta
 * diz "um aviso por dia, no máximo".
 */
class AvisoDeConsumoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        if (!prefs.avisosAtivos) return Result.success()

        AvisoDeConsumo.criarCanal(applicationContext)

        val hoje = HorarioDoDia.diaDeHoje()
        if (prefs.ultimoAvisoConsumoDia == hoje) {
            // Já avisamos hoje. Não empilha aviso: agenda a próxima rodada e sai.
            AvisoDeConsumo.agendarProxima(applicationContext)
            return Result.success()
        }
        if (ultimoAvisoDeValidadeFoiHoje(hoje)) {
            AvisoDeConsumo.agendarProxima(applicationContext)
            return Result.success()
        }

        val banco = StockUpDatabase.getInstance(applicationContext)
        val historico = ConsumoRepository(banco.consumoDao())
        val agora = System.currentTimeMillis()
        val moradores = prefs.moradores

        // Laço com suspend dentro: as consultas ao banco agora são suspend, e um
        // mapNotNull (lambda comum) não aceitaria.
        val candidatos = mutableListOf<Candidato>()
        for (produto in banco.productDao().getAllProducts().first()) {
            // Produto com validade tem o aviso de vencimento; este aqui é outro papo.
            if (produto.hasExpiration && produto.expirationDate > 0L) continue

            val quantidade = CurrencyUtils.parseQuantity(produto.quantity, fallback = 0.0)
            if (quantidade <= 0.0 || produto.name.isBlank()) continue

            val chave = ConsumptionEngine.chaveDoProduto(produto.name, produto.unit)
            val registro = historico.aviso(chave)
            if (registro.proximoAviso > agora) continue

            val estimativa = ConsumptionEngine.estimar(
                quantidadeAtual = quantidade,
                categoria = produto.category,
                moradores = moradores,
                observacoes = historico.observacoes(chave)
            )
            if (!ConsumptionEngine.deveAvisar(estimativa.diasRestantes)) continue

            // Comprado agora e ainda sem histórico? Deixa passar um dia — avisar
            // "vai acabar" no dia da compra é o tipo de aviso que faz desinstalar.
            val diasDesdeCadastro = (agora - produto.createdAt).toDouble() / DIA_MS
            if (estimativa.origem == OrigemEstimativa.CATEGORIA && diasDesdeCadastro < 1.0) continue

            candidatos += Candidato(produto, chave, estimativa)
        }
        candidatos.sortBy { it.estimativa.diasRestantes }

        val escolhido = candidatos.firstOrNull()
        if (escolhido == null) {
            AvisoDeConsumo.agendarProxima(applicationContext)
            return Result.success()
        }

        val registro = historico.aviso(escolhido.chave)
        val intervaloDias = ConsumptionEngine.intervaloMinimoDias(registro.ignorados)
        notificar(escolhido, moradores, intervaloDias)

        historico.registrarAviso(
            chave = escolhido.chave,
            proximo = agora + intervaloDias * DIA_MS.toLong()
        )
        prefs.ultimoAvisoConsumoDia = hoje

        AvisoDeConsumo.agendarProxima(applicationContext)
        return Result.success()
    }

    private fun ultimoAvisoDeValidadeFoiHoje(hoje: Int): Boolean =
        UserPreferences(applicationContext).ultimoAvisoValidadeDia == hoje

    private fun notificar(candidato: Candidato, moradores: Int, intervaloDias: Int) {
        val produto = candidato.produto
        val quando = textoDeQuando(candidato.estimativa.diasRestantes)
        val explicacao = when (candidato.estimativa.origem) {
            OrigemEstimativa.HISTORICO ->
                "Pelo seu consumo (${candidato.estimativa.amostras}x), deve acabar $quando."
            OrigemEstimativa.CATEGORIA ->
                "Não tenho histórico deste produto ainda. Para uma casa de $moradores " +
                    "${if (moradores == 1) "pessoa" else "pessoas"}, deve acabar $quando."
        }

        val abrir = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val aviso = NotificationCompat.Builder(applicationContext, AvisoDeConsumo.CANAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${produto.name} está acabando?")
            .setContentText("$explicacao Ainda tem?")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$explicacao Ainda tem?"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                "Ainda tenho",
                acao(produto.id, AvisoConsumoReceiver.ACAO_AINDA_TENHO, 1)
            )
            .addAction(
                R.drawable.ic_notification,
                "Está acabando",
                acao(produto.id, AvisoConsumoReceiver.ACAO_ACABANDO, 2)
            )
            .addAction(
                R.drawable.ic_notification,
                "Já acabou",
                acao(produto.id, AvisoConsumoReceiver.ACAO_ACABOU, 3)
            )
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(AvisoDeConsumo.AVISO_ID, aviso)
        } catch (e: SecurityException) {
            // Sem permissão de notificação, a tela de Avisos cobre o recado.
        }
    }

    /** "hoje", "amanhã" ou "há X dias" — em palavras, como a Carta pede. */
    private fun textoDeQuando(diasRestantes: Double): String = when {
        diasRestantes <= 0.0 -> "já passou do previsto"
        diasRestantes < 1.5 -> "hoje"
        diasRestantes < 2.5 -> "amanhã"
        else -> "em ${ConsumptionEngine.diasInteiros(diasRestantes)} dias"
    }

    private fun acao(produtoId: Long, tipo: String, indice: Int): PendingIntent {
        val intent = Intent(applicationContext, AvisoConsumoReceiver::class.java).apply {
            putExtra(AvisoConsumoReceiver.EXTRA_ACAO, tipo)
            putExtra(AvisoConsumoReceiver.EXTRA_PRODUTO_ID, produtoId)
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            produtoId.toInt() * 10 + indice,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class Candidato(
        val produto: Product,
        val chave: String,
        val estimativa: com.example.stockup.domain.Estimativa
    )

    private companion object {
        const val DIA_MS = 24.0 * 60.0 * 60.0 * 1000.0
    }
}
