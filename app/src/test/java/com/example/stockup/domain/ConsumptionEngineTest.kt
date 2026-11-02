package com.example.stockup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do motor de consumo.
 *
 * É a parte do app que "aprende" o ritmo da casa — e a única que dá para provar
 * com números em vez de esperar dois dias. Se a média estiver errada, o aviso
 * chega na hora errada e a pessoa perde a confiança no app inteiro.
 */
class ConsumptionEngineTest {

    private val delta = 0.0001

    private fun obs(quantidade: Double, dias: Double, quando: Long) =
        ObservacaoConsumo(chave = "leite|un", quantidade = quantidade, dias = dias, quando = quando)

    // ─────────────── sem histórico: categoria + moradores ───────────────

    @Test
    fun semHistoricoUsaOPadraoDaCategoria() {
        // Laticínio: 7 dias para uma pessoa. 1 litro deve durar 7 dias.
        val estimativa = ConsumptionEngine.estimar(
            quantidadeAtual = 1.0,
            categoria = "Laticínios",
            moradores = 1,
            observacoes = emptyList()
        )
        assertEquals(7.0, estimativa.diasRestantes, delta)
        assertEquals(OrigemEstimativa.CATEGORIA, estimativa.origem)
    }

    @Test
    fun maisMoradoresConsomemMaisRapido() {
        // Três pessoas comem o mesmo queijo em ~1/3 do tempo.
        val estimativa = ConsumptionEngine.estimar(
            quantidadeAtual = 1.0,
            categoria = "Laticínios",
            moradores = 3,
            observacoes = emptyList()
        )
        assertEquals(7.0 / 3.0, estimativa.diasRestantes, delta)
    }

    @Test
    fun carneDuraMenosQueSabao() {
        // O padrão por categoria existe para diferenciar o que estraga rápido
        // do que dura meses.
        val carne = ConsumptionEngine.duracaoPadraoPorCategoria("Carnes")
        val limpeza = ConsumptionEngine.duracaoPadraoPorCategoria("Limpeza")
        assertTrue("carne ($carne) deveria durar menos que limpeza ($limpeza)", carne < limpeza)
    }

    @Test
    fun categoriaDesconhecidaTemPadraoRazoavel() {
        // Sem categoria cadastrada, nada de zero nem de infinito.
        val dias = ConsumptionEngine.duracaoPadraoPorCategoria("")
        assertTrue("padrão genérico deveria ser positivo: $dias", dias > 0.0)
    }

    // ─────────────── com histórico ───────────────

    @Test
    fun umLitroEmDoisDiasDaMeioLitroPorDia() {
        // Exemplo do pedido do dono: 1 litro consumido em 2 dias = 0,5/dia.
        // Uma nova caixa de 1 litro dura ~2 dias.
        val estimativa = ConsumptionEngine.estimar(
            quantidadeAtual = 1.0,
            categoria = "Laticínios",
            moradores = 1,
            observacoes = listOf(obs(1.0, 2.0, 1_000L))
        )
        assertEquals(0.5, estimativa.mediaDiaria, delta)
        assertEquals(2.0, estimativa.diasRestantes, delta)
        assertEquals(OrigemEstimativa.HISTORICO, estimativa.origem)
    }

    @Test
    fun historicoVenceOPadraoDaCategoria() {
        // Uma pessoa que consome leite em 1 dia não pode receber "dura 7 dias"
        // só porque é laticínio. Com histórico, o padrão é ignorado.
        val estimativa = ConsumptionEngine.estimar(
            quantidadeAtual = 1.0,
            categoria = "Laticínios",
            moradores = 1,
            observacoes = listOf(obs(1.0, 1.0, 1_000L))
        )
        assertEquals(OrigemEstimativa.HISTORICO, estimativa.origem)
        assertEquals(1.0, estimativa.diasRestantes, delta)
    }

    @Test
    fun mediaNaoEDefinidaPeloUltimoRegistro() {
        // Duas compras normais (2 dias cada) e uma atípica (10 dias).
        // A média tem de ficar perto de 2, não em 10.
        val observacoes = listOf(
            obs(1.0, 10.0, 3_000L), // mais antiga
            obs(1.0, 2.0, 2_000L),
            obs(1.0, 2.0, 1_000L)
        )
        val media = ConsumptionEngine.mediaDiaria(observacoes)
        assertTrue("média puxada demais pelo registro atípico: $media", media > 0.25)
        assertTrue("média deveria ficar perto de 0,5/dia: $media", media < 0.6)
    }

    @Test
    fun observacaoRecentePesaMaisQueAAntiga() {
        // Antes: 1 por dia. Agora: 1 a cada 5 dias (a casa passou a consumir menos).
        // A previsão tem de acompanhar a mudança, não ficar presa no passado.
        val observacoes = listOf(
            obs(1.0, 5.0, 2_000L), // recente
            obs(1.0, 1.0, 1_000L)  // antiga
        )
        val media = ConsumptionEngine.mediaDiaria(observacoes)
        assertTrue("a média deveria ficar entre 1/5 e 1 por dia: $media", media in 0.2..1.0)
        assertTrue("deveria puxar para o consumo recente (mais lento): $media", media < 0.67)
    }

    @Test
    fun observacaoComDiasZeroEhIgnorada() {
        // Divisão por zero envenenaria a média; o registro inválido é descartado.
        val media = ConsumptionEngine.mediaDiaria(listOf(obs(1.0, 0.0, 1_000L)))
        assertEquals(0.0, media, delta)
    }

    @Test
    fun observacaoComQuantidadeZeroEhIgnorada() {
        val media = ConsumptionEngine.mediaDiaria(listOf(obs(0.0, 2.0, 1_000L)))
        assertEquals(0.0, media, delta)
    }

    @Test
    fun usaNoMaximoAsCincoUltimasObservacoes() {
        // Histórico velho demais não pode segurar a previsão de hoje.
        val observacoes = (1..10).map { indice ->
            obs(1.0, 1.0, indice.toLong() * 1000L)
        }
        val estimativa = ConsumptionEngine.estimar(1.0, "Despensa", 1, observacoes)
        assertEquals(ConsumptionEngine.MAX_AMOSTRAS, estimativa.amostras)
    }

    @Test
    fun quantidadeZeroDaZeroDias() {
        val estimativa = ConsumptionEngine.estimar(0.0, "Despensa", 2, emptyList())
        assertEquals(0.0, estimativa.diasRestantes, delta)
    }

    // ─────────────── quando avisar ───────────────

    @Test
    fun avisaQuandoFaltaUmDiaOuMenos() {
        assertTrue(ConsumptionEngine.deveAvisar(1.0))
        assertTrue(ConsumptionEngine.deveAvisar(0.2))
        assertTrue("previsão já passou: tem de avisar", ConsumptionEngine.deveAvisar(-2.0))
    }

    @Test
    fun naoAvisaLongeDoFim() {
        // Quatro dias de folga é cedo demais: avisar aí ensina a ignorar o app.
        assertTrue(!ConsumptionEngine.deveAvisar(4.0))
        assertTrue(!ConsumptionEngine.deveAvisar(2.5))
    }

    // ─────────────── intervalo entre avisos ───────────────

    @Test
    fun intervaloDobraACadaAvisoIgnorado() {
        // 1, 2, 4, 8 e depois 14 (teto). É o que impede o app de virar alarme.
        assertEquals(1, ConsumptionEngine.intervaloMinimoDias(0))
        assertEquals(2, ConsumptionEngine.intervaloMinimoDias(1))
        assertEquals(4, ConsumptionEngine.intervaloMinimoDias(2))
        assertEquals(8, ConsumptionEngine.intervaloMinimoDias(3))
        assertEquals(14, ConsumptionEngine.intervaloMinimoDias(9))
    }

    // ─────────────── chave do produto ───────────────

    @Test
    fun chaveIgnoraAcentoEMaiuscula() {
        val a = ConsumptionEngine.chaveDoProduto("Leite Cemil", "L")
        val b = ConsumptionEngine.chaveDoProduto("leite cemil ", "l")
        assertEquals(a, b)
    }

    @Test
    fun chaveSeparaUnidadesDiferentes() {
        // "Leite" em litro e "Leite" em unidade são produtos diferentes para a média.
        val litro = ConsumptionEngine.chaveDoProduto("Leite", "L")
        val unidade = ConsumptionEngine.chaveDoProduto("Leite", "un")
        assertTrue(litro != unidade)
    }
}
