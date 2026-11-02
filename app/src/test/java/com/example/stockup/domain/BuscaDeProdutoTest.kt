package com.example.stockup.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes da busca da Despensa.
 *
 * Regressão que originou isto (vista no aparelho, 02/11): procurar "classico"
 * devolvia "Nenhum resultado" mesmo existindo "Azeite ... Clássico" na despensa.
 * Teclado de celular em português quase não tem acento — a busca tem de resolver.
 */
class BuscaDeProdutoTest {

    private fun acha(consulta: String, nome: String = "Azeite de Oliva Extravirgem Clássico (Gallo)") =
        BuscaDeProduto.combina(nome = nome, local = "Armário", categoria = "Despensa", consulta = consulta)

    @Test
    fun achaSemAcentoMesmoComAcentoNoProduto() {
        assertTrue(acha("classico"))
    }

    @Test
    fun achaComAcento() {
        assertTrue(acha("clássico"))
    }

    @Test
    fun maiusculaNaoImporta() {
        assertTrue(acha("AZEITE"))
        assertTrue(acha("Azeite"))
    }

    @Test
    fun espacoSobrandoNaoQuebra() {
        // Regressão: "leite " (com espaço no fim) não achava nada.
        assertTrue(acha("azeite   "))
        assertTrue(acha("  azeite"))
    }

    @Test
    fun pedacoDoNomeBasta() {
        assertTrue(acha("gallo"))
        assertTrue(acha("oliva"))
    }

    @Test
    fun buscaPeloLocal() {
        // Quem esqueceu o nome procura pelo lugar: "armário" acha o que está lá.
        assertTrue(acha("armario"))
    }

    @Test
    fun buscaPelaCategoria() {
        assertTrue(acha("despensa"))
    }

    @Test
    fun consultaVaziaMostraTudo() {
        assertTrue(acha(""))
        assertTrue(acha("   "))
    }

    @Test
    fun naoAchaOQueNaoExiste() {
        assertFalse(acha("chocolate"))
        assertFalse(acha("zzz"))
    }

    @Test
    fun acentoNaConsultaTambemEhAceito() {
        assertTrue(BuscaDeProduto.combina("Açúcar Refinado", "Despensa", "Despensa", "acucar"))
        assertTrue(BuscaDeProduto.combina("Açúcar Refinado", "Despensa", "Despensa", "açúcar"))
    }
}
