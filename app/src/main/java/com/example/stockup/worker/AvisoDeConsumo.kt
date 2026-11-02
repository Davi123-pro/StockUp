package com.example.stockup.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stockup.domain.HorarioDoDia
import com.example.stockup.utils.UserPreferences
import java.util.concurrent.TimeUnit

/**
 * O aviso de consumo: "isto está acabando".
 *
 * Diferente do aviso de validade (que olha a data impressa na embalagem), este olha
 * o RITMO da casa: quanto se consome por dia, quanto ainda tem, e quando deve
 * acabar. Um intervalo fixo ("avise a cada 7 dias") erra os dois lados — avisa
 * cedo demais para quem consome rápido e tarde demais para quem consome devagar.
 *
 * Como funciona: o app acorda UMA vez por dia, no horário que a pessoa escolheu,
 * e olha a despensa inteira. Só avisa do produto cuja previsão de término chegou
 * (ou está a menos de um dia), e só de um por dia. Cada produto tem o seu próprio
 * intervalo mínimo, que dobra a cada aviso sem resposta.
 *
 * Canal e id próprios: é aviso de consumo, não de validade, e não aparece na tela
 * de Avisos do app (que continua sendo só sobre vencimento).
 */
object AvisoDeConsumo {

    const val CANAL_ID = "stockup_consumo_canal"

    /** Id do aviso. Separado do 1001 (validade) e do antigo 2001 (perguntas). */
    const val AVISO_ID = 3001

    private const val TRABALHO = "stockup_aviso_consumo"

    fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_ID,
            "Avisos de consumo",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisa quando um produto está perto de acabar, pelo seu próprio ritmo de consumo"
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canal)
    }

    /** Reagenda do zero (quando muda a hora, liga/desliga ou abre o app). */
    fun reagendar(context: Context) {
        agendar(context, ExistingWorkPolicy.REPLACE)
    }

    /**
     * Continua a fila depois de uma rodada.
     *
     * `APPEND_OR_REPLACE` porque é chamado de dentro do worker que acabou de rodar:
     * `REPLACE` cancelaria o trabalho de mesmo nome — inclusive ele mesmo.
     */
    fun agendarProxima(context: Context) {
        agendar(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun agendar(context: Context, politica: ExistingWorkPolicy) {
        val prefs = UserPreferences(context)
        if (!prefs.avisosAtivos) {
            cancelar(context)
            return
        }
        criarCanal(context)

        val pedido = OneTimeWorkRequestBuilder<AvisoDeConsumoWorker>()
            .setInitialDelay(
                // A hora não vem de configuração: sai do costume de uso, e nunca
                // cai entre meia-noite e 5h.
                HorarioDoDia.msAte(prefs.horaDoAviso() * 60, System.currentTimeMillis()),
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(TRABALHO, politica, pedido)
    }

    /** Usado quando a pessoa desliga os avisos: para tudo e limpa o que está na tela. */
    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABALHO)
        context.getSystemService(NotificationManager::class.java)?.cancel(AVISO_ID)
    }
}
