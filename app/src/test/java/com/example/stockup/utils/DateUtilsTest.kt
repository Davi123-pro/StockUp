package com.example.stockup.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Testes de `DateUtils`.
 *
 * Regressão mais importante: o DatePicker do Material 3 devolve MEIA-NOITE EM UTC,
 * mas o app exibe e compara tudo no fuso LOCAL. No Brasil (UTC-3) isso fazia
 * "15/11/2026" aparecer como "14/11/2026" e o alerta de validade disparar
 * um dia adiantado. `utcPickerMillisToLocalDay` existe para corrigir isso.
 *
 * Os testes são escritos para valer em QUALQUER fuso: nada aqui assume UTC-3.
 */
class DateUtilsTest {

    // ------------------------------------------------------ utcPickerMillisToLocalDay

    @Test
    fun dataDoPickerNaoVoltaUmDiaAtras() {
        // Regressão: a data escolhida no DatePicker (meia-noite UTC do dia D) tem de
        // ser exibida como o dia D, e não como D-1 no fuso brasileiro.
        val meiaNoiteUtcDoDia15 = meiaNoiteUtc(2026, Calendar.NOVEMBER, 15)
        val convertido = DateUtils.utcPickerMillisToLocalDay(meiaNoiteUtcDoDia15)

        assertEquals("15/11/2026", DateUtils.formatDate(convertido))
    }

    @Test
    fun dataDoPickerViraMeiaNoiteLocalDoMesmoDia() {
        // Regressão: o valor gravado no banco precisa ser a meia-noite LOCAL do dia
        // escolhido — meia-noite UTC gravada crua desloca o dia em UTC-3.
        val meiaNoiteUtcDoDia15 = meiaNoiteUtc(2026, Calendar.NOVEMBER, 15)
        val convertido = DateUtils.utcPickerMillisToLocalDay(meiaNoiteUtcDoDia15)

        assertEquals(meiaNoiteLocal(2026, Calendar.NOVEMBER, 15), convertido)
    }

    @Test
    fun dataDoPickerPreservaOsDiasAteVencer() {
        // Regressão: se a conversão UTC->local andar um dia, o alerta de validade
        // dispara um dia antes/depois do correto. Uma data de hoje+5 tem de dar 5.
        val meiaNoiteUtcDeHojeMais5 = meiaNoiteUtcDoDiaLocal(DateUtils.daysFromNow(5))
        val convertido = DateUtils.utcPickerMillisToLocalDay(meiaNoiteUtcDeHojeMais5)

        // Observação: em fusos com horário de verão, se a transição cair entre hoje e
        // hoje+5 a conta em milissegundos pode truncar para 4. O Brasil não usa mais
        // horário de verão desde 2019, então 5 é o resultado esperado aqui.
        assertEquals(5L, DateUtils.daysUntilExpiration(convertido))
    }

    // ------------------------------------------------------ startOfToday

    @Test
    fun inicioDeHojeEhAMeiaNoiteLocal() {
        // Regressão: startOfToday() é a base de daysUntilExpiration; se não for
        // exatamente a meia-noite local, "vence hoje" deixa de dar 0.
        val agora = Calendar.getInstance()
        val esperado = meiaNoiteLocal(
            agora.get(Calendar.YEAR),
            agora.get(Calendar.MONTH),
            agora.get(Calendar.DAY_OF_MONTH)
        )

        assertEquals(esperado, DateUtils.startOfToday())
    }

    @Test
    fun inicioDeHojeNuncaEstaNoFuturoEVenceHoje() {
        // Regressão: a meia-noite de hoje conta como "vence hoje" (0 dias) e nunca
        // pode ser um instante futuro.
        assertEquals(0L, DateUtils.daysUntilExpiration(DateUtils.startOfToday()))
        assertTrue(DateUtils.startOfToday() <= System.currentTimeMillis())
    }

    // ------------------------------------------------------ formatDate

    @Test
    fun formatDateMostraODiaNoFusoLocal() {
        // Regressão: o formatador é local (SimpleDateFormat sem TimeZone) — é
        // exatamente por isso que meia-noite UTC virava o dia anterior.
        assertEquals(
            "15/11/2026",
            DateUtils.formatDate(meiaNoiteLocal(2026, Calendar.NOVEMBER, 15))
        )
    }

    // ------------------------------------------------------ daysUntilExpiration

    @Test
    fun diasAteVencerNoFuturoEhPositivo() {
        // Regressão: três dias à frente precisa dar exatamente 3.
        assertEquals(3L, DateUtils.daysUntilExpiration(DateUtils.daysFromNow(3)))
    }

    @Test
    fun diasAteVencerNoPassadoEhNegativo() {
        // Regressão: data vencida precisa vir NEGATIVA ("Venceu há X dias"); devolver
        // 0 faria o app dizer "Vence hoje!" para comida já vencida.
        // (Assumindo que não há mudança de horário de verão dentro de 2 dias.)
        val doisDiasAtras = DateUtils.startOfToday() - 2 * MILLIS_POR_DIA
        assertEquals(-2L, DateUtils.daysUntilExpiration(doisDiasAtras))
    }

    // ------------------------------------------------------ expirationText

    @Test
    fun textoDeVencimentoHoje() {
        // Regressão: 0 dias -> "Vence hoje!" (o caso do alerta diário).
        assertEquals("Vence hoje!", DateUtils.expirationText(DateUtils.startOfToday()))
    }

    @Test
    fun textoDeVencimentoAmanha() {
        // Regressão: 1 dia -> "Vence amanhã", sem número.
        assertEquals("Vence amanhã", DateUtils.expirationText(DateUtils.daysFromNow(1)))
    }

    @Test
    fun textoDeVencimentoOntem() {
        // Regressão: -1 dia -> "Venceu ontem" (e não "Venceu há 1 dias").
        val ontem = DateUtils.startOfToday() - MILLIS_POR_DIA
        assertEquals("Venceu ontem", DateUtils.expirationText(ontem))
    }

    @Test
    fun textoDeVencimentoDistante() {
        // Regressão: prazos longos usam a forma "Vence em N dias".
        assertTrue(DateUtils.expirationText(DateUtils.daysFromNow(30)).startsWith("Vence em "))
    }
}

private const val MILLIS_POR_DIA = 86_400_000L

/** Meia-noite UTC (00:00:00.000) do dia pedido — é o que o DatePicker do M3 devolve. */
private fun meiaNoiteUtc(ano: Int, mes: Int, dia: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(ano, mes, dia, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** Meia-noite local (00:00:00.000) do dia pedido, igual ao que a produção grava. */
private fun meiaNoiteLocal(ano: Int, mes: Int, dia: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(ano, mes, dia, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * Meia-noite UTC do MESMO dia de calendário LOCAL do instante [millis].
 *
 * Não dá para converter [millis] para UTC e zerar o dia: em UTC-3, "hoje+5 às
 * 23:59:59.999" já é 02:59:59.999 de hoje+6 em UTC, e o teste passaria a exigir
 * 6 dias em vez de 5. O DatePicker devolve a meia-noite UTC do dia que o usuário
 * tocou no calendário dele, ou seja, do dia local.
 */
private fun meiaNoiteUtcDoDiaLocal(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
