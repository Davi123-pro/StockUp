package com.example.stockup.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DateUtils {

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    fun formatDate(timestamp: Long): String =
        displayFormat.format(Date(timestamp))

    /** Meia-noite de hoje, no fuso local. */
    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * O DatePicker do Material 3 devolve meia-noite em UTC. Como exibimos e comparamos
     * no fuso local (Brasil, UTC-3), a data escolhida aparecia um dia antes
     * ("escolhi 15/11, mostrou 14/11") e o alerta de validade disparava adiantado.
     * Converte para a meia-noite LOCAL do dia que o usuário realmente tocou.
     */
    fun utcPickerMillisToLocalDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Retorna quantos dias faltam para o vencimento. Negativo = já venceu. */
    fun daysUntilExpiration(expirationTimestamp: Long): Long {
        val now = startOfToday()

        val expiration = Calendar.getInstance().apply {
            timeInMillis = expirationTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return TimeUnit.MILLISECONDS.toDays(expiration - now)
    }

    /** Retorna texto humanizado: "Vence em X dias", "Vence hoje", "Venceu há X dias" */
    fun expirationText(expirationTimestamp: Long): String {
        val days = daysUntilExpiration(expirationTimestamp)
        return when {
            days > 1L -> "Vence em $days dias"
            days == 1L -> "Vence amanhã"
            days == 0L -> "Vence hoje!"
            days == -1L -> "Venceu ontem"
            else -> "Venceu há ${-days} dias"
        }
    }

    /** Retorna timestamp do início do dia daqui a N dias */
    fun daysFromNow(days: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
}
