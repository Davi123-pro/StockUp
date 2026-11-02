package com.example.stockup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes da escolha da hora do aviso.
 *
 * O dono pediu: o app escolhe a melhor hora, e nunca entre meia-noite e 5 da
 * manhã. Se essa regra quebrar, o celular apita de madrugada — e uma vez basta
 * para a pessoa desligar os avisos para sempre.
 */
class EscolhaDeHorarioTest {

    private fun usos(vararg pares: Pair<Int, Int>): IntArray {
        val vetor = IntArray(24)
        pares.forEach { (hora, vezes) -> vetor[hora] = vezes }
        return vetor
    }

    @Test
    fun semHistoricoUsaNoveDaManha() {
        assertEquals(9, EscolhaDeHorario.melhorHora(IntArray(24)))
    }

    @Test
    fun escolheAHoraMaisUsada() {
        // Abre mais às 20h do que de manhã: o aviso vai às 20h.
        assertEquals(20, EscolhaDeHorario.melhorHora(usos(7 to 2, 20 to 9)))
    }

    @Test
    fun madrugadaNuncaEhEscolhida() {
        // A pessoa só abriu o app às 3h (insônia, troca de turno...). O aviso NÃO
        // pode ir às 3h: cai no padrão das 9h.
        assertEquals(9, EscolhaDeHorario.melhorHora(usos(3 to 30)))
    }

    @Test
    fun madrugadaNaoVenceNemQuandoExisteUsoDeDia() {
        assertEquals(18, EscolhaDeHorario.melhorHora(usos(4 to 50, 18 to 3)))
    }

    @Test
    fun empateEscolheAHoraMaisCedo() {
        // Abre igual às 8h e às 21h. Aviso cedo dá tempo de agir no mesmo dia.
        assertEquals(8, EscolhaDeHorario.melhorHora(usos(21 to 5, 8 to 5)))
    }

    @Test
    fun limitesDaJanelaSaoPermitidos() {
        assertTrue(EscolhaDeHorario.horaPermitida(6))
        assertTrue(EscolhaDeHorario.horaPermitida(23))
        assertTrue(!EscolhaDeHorario.horaPermitida(5))
        assertTrue(!EscolhaDeHorario.horaPermitida(0))
    }

    @Test
    fun vetorInvalidoCaiNoPadrao() {
        // Estado corrompido (texto cortado, por exemplo) não pode quebrar o app.
        assertEquals(9, EscolhaDeHorario.melhorHora(IntArray(3)))
    }
}
