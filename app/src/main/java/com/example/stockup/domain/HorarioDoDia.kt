package com.example.stockup.domain

import java.util.Calendar

/**
 * Conta quanto falta para o relógio marcar [minutoDoDia] (9:00 = 540).
 *
 * Vive separado do worker porque é a única parte do agendamento que dá para
 * testar sem Android — e é justamente onde um erro faria o app avisar na hora
 * errada (ou nunca).
 */
object HorarioDoDia {

    /** Espera até a próxima vez que o relógio marcar [minutoDoDia] (hoje ou amanhã). */
    fun msAte(minutoDoDia: Int, agora: Long): Long {
        val alvo = Calendar.getInstance().apply {
            timeInMillis = agora
            set(Calendar.HOUR_OF_DAY, (minutoDoDia / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (minutoDoDia % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alvo.timeInMillis <= agora) {
            alvo.add(Calendar.DAY_OF_MONTH, 1)
        }
        return alvo.timeInMillis - agora
    }

    /** Dia no formato AAAAMMDD, usado para "um aviso por dia". */
    fun diaDeHoje(agora: Long = System.currentTimeMillis()): Int =
        Calendar.getInstance().apply { timeInMillis = agora }.let { cal ->
            val ano = cal.get(Calendar.YEAR)
            val mes = cal.get(Calendar.MONTH) + 1
            val dia = cal.get(Calendar.DAY_OF_MONTH)
            ano * 10000 + mes * 100 + dia
        }
}
