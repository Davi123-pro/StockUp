package com.example.stockup.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockup.MainActivity
import com.example.stockup.R
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.StockUpDatabase
import com.example.stockup.domain.HorarioDoDia
import com.example.stockup.utils.DateUtils
import com.example.stockup.utils.UserPreferences
import kotlin.math.abs

/**
 * Avisa sobre o produto mais urgente do dia, com as ações "Acabei" e "Joguei fora".
 *
 * Por que UM produto e não a lista inteira: para as ações funcionarem, cada
 * notificação precisa saber de QUAL produto se trata. A primeira versão fazia uma
 * por produto, agrupadas — e o teste no aparelho mostrou o defeito: com grupo, as
 * ações só aparecem depois de arrastar a notificação para baixo. Para o nosso
 * público, "arraste para ver os botões" é o mesmo que "não tem botão".
 *
 * Agora sai uma só, a mais urgente, com as ações à vista. O restante aparece no
 * texto ("Mais 2 na lista") e na tela de Avisos. Quando o usuário resolve a de
 * hoje, amanhã surge a próxima — a fila anda sozinha.
 */
class ExpirationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // O canal tem de existir ANTES de qualquer notify().
        criarCanalDeNotificacao(applicationContext)

        val productDao = StockUpDatabase.getInstance(applicationContext).productDao()

        // Janela: de 30 dias atrás até 3 dias à frente. O piso evita que um produto
        // vencido há meses gere o mesmo aviso todos os dias, para sempre.
        val threshold = DateUtils.daysFromNow(3)
        val piso = DateUtils.daysFromNow(-30)

        val produtos = productDao.getProductsExpiringSoon(threshold, piso)
        if (produtos.isEmpty()) {
            NotificationManagerCompat.from(applicationContext).cancel(NOTIFICACAO_ID)
            return Result.success()
        }

        // O mais próximo de hoje: um produto que vence amanhã é mais acionável que
        // um que venceu há 20 dias.
        val maisUrgente = produtos.minByOrNull {
            abs(DateUtils.daysUntilExpiration(it.expirationDate))
        } ?: return Result.success()

        notificar(maisUrgente, totalNaLista = produtos.size)
        return Result.success()
    }

    private fun notificar(produto: Product, totalNaLista: Int) {
        val dias = DateUtils.daysUntilExpiration(produto.expirationDate)
        val situacao = when {
            dias < 0L -> if (-dias == 1L) "Venceu ontem." else "Venceu há ${-dias} dias."
            dias == 0L -> "Vence hoje."
            dias == 1L -> "Vence amanhã."
            else -> "Vence em $dias dias."
        }
        val restantes = totalNaLista - 1
        val texto = when {
            restantes <= 0 -> situacao
            restantes == 1 -> "$situacao Mais 1 na lista."
            else -> "$situacao Mais $restantes na lista."
        }

        val abrir = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val aviso = NotificationCompat.Builder(applicationContext, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            // O nome do produto é o título: é o que a pessoa reconhece de longe.
            .setContentTitle(produto.name)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                "Acabei",
                pendenteDeAcao(produto, AcaoRapidaReceiver.ACAO_USEI, 1)
            )
            .addAction(
                R.drawable.ic_notification,
                "Joguei fora",
                pendenteDeAcao(produto, AcaoRapidaReceiver.ACAO_JOGUEI_FORA, 2)
            )
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICACAO_ID, aviso)
            // Marca o dia para o aviso de consumo não somar um segundo aviso hoje:
            // a Carta permite um aviso por dia, e validade tem prioridade.
            UserPreferences(applicationContext).ultimoAvisoValidadeDia =
                HorarioDoDia.diaDeHoje()
        } catch (e: SecurityException) {
            // Sem POST_NOTIFICATIONS não há o que mostrar; a tela de Avisos cobre.
        }
    }

    /**
     * Código diferente por produto E por ação — sem isso um PendingIntent sobrescreve
     * o outro e o "Joguei fora" acabaria fazendo o que o "Acabei" faz.
     */
    private fun codigoDe(produto: Product, indice: Int): Int = produto.id.toInt() * 10 + indice

    private fun pendenteDeAcao(
        produto: Product,
        acao: String,
        indice: Int
    ): PendingIntent {
        val intent = Intent(applicationContext, AcaoRapidaReceiver::class.java).apply {
            putExtra(AcaoRapidaReceiver.EXTRA_ACAO, acao)
            putExtra(AcaoRapidaReceiver.EXTRA_PRODUTO_ID, produto.id)
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            codigoDe(produto, indice),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CANAL_ID = "stockup_expiration_channel"

        /** Id fixo: o aviso de hoje substitui o de ontem, em vez de empilhar. */
        const val NOTIFICACAO_ID = 1001

        /**
         * Cria o canal de notificação. Chamado no início do app (MainActivity) e
         * também pelo worker, para o caso de o app nunca ter sido aberto.
         */
        fun criarCanalDeNotificacao(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nome = "Avisos de validade"
            val descricao = "Avisos sobre produtos perto de vencer ou vencidos"
            val canal = NotificationChannel(
                CANAL_ID,
                nome,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = descricao }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(canal)
        }
    }
}
