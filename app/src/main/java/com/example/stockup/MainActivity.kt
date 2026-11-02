package com.example.stockup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stockup.theme.StockUpTheme
import com.example.stockup.utils.UserPreferences
import com.example.stockup.worker.AvisoDeConsumo
import com.example.stockup.worker.ExpirationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O canal precisa existir antes de qualquer notificação — inclusive para o
        // pedido de permissão do sistema aparecer no Android 12 e anterior.
        ExpirationWorker.criarCanalDeNotificacao(applicationContext)
        setupExpirationWorker()

        // Aviso de consumo (produto perto de acabar pelo ritmo da casa): canal e
        // agendamento próprios. Reagendar a cada abertura conserta o caso de o
        // celular ter reiniciado com o trabalho perdido.
        AvisoDeConsumo.criarCanal(applicationContext)
        // Antes de reagendar: toda abertura conta para o app descobrir a melhor
        // hora de falar com a pessoa (nunca de madrugada).
        UserPreferences(applicationContext).registrarUso()
        AvisoDeConsumo.reagendar(applicationContext)

        enableEdgeToEdge()
        setContent {
            StockUpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private fun setupExpirationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ExpirationWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "stockup_expiration_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
