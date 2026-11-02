package com.example.stockup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Testes do relógio dos avisos.
 *
 * O erro aqui é o pior tipo: silencioso. Agendar no passado faz o app acordar em
 * loop; agendar 24 h a mais faz o aviso chegar depois de o produto acabar.
 */
class HorarioDoDiaTest {

    private val horaEmMs = 60L * 60L * 1000L

    private fun hoje(hora: Int, minuto: Int = 0, segundo: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, segundo)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun esperaAteAs9QuandoSao8() {
        assertEquals(horaEmMs, HorarioDoDia.msAte(9 * 60, hoje(8, 0)))
    }

    @Test
    fun depoisDas9SoAmanha() {
        assertEquals(23 * horaEmMs, HorarioDoDia.msAte(9 * 60, hoje(10, 0)))
    }

    @Test
    fun horarioExatoNaoDisparaDuasVezes() {
        // Às 09:00:00 em ponto, a próxima é amanhã. Sem isso, abrir o app às 9:00
        // dispararia o aviso na hora e o agendamento ficaria no passado.
        assertEquals(24 * horaEmMs, HorarioDoDia.msAte(9 * 60, hoje(9, 0)))
    }

    @Test
    fun segundosAntesAindaEsperaPouco() {
        assertEquals(30_000L, HorarioDoDia.msAte(9 * 60, hoje(8, 59, 30)))
    }

    @Test
    fun atrasoNuncaEhZeroNemNegativo() {
        for (hora in 0..23) {
            val agora = hoje(hora, 30)
            val atraso = HorarioDoDia.msAte(9 * 60, agora)
            assertTrue("atraso inválido às ${hora}h: $atraso", atraso > 0L)
            assertTrue("atraso maior que um dia às ${hora}h: $atraso", atraso <= 24 * horaEmMs)
        }
    }

    @Test
    fun diaDeHojeMudaAAoMudarDeDia() {
        val dia = HorarioDoDia.diaDeHoje()
        val amanha = HorarioDoDia.diaDeHoje(System.currentTimeMillis() + 24 * horaEmMs)
        assertTrue("o dia precisa avançar em 24h", amanha > dia)
    }
}
