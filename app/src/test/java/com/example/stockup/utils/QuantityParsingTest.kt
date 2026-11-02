package com.example.stockup.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de `CurrencyUtils.parseQuantity` e do total por item.
 *
 * Regressão principal: o teclado decimal do Android em pt-BR digita VÍRGULA.
 * Quando o leitor era `quantity.toDoubleOrNull() ?: 1.0`, "1,5" virava null e era
 * silenciosamente tratado como 1.0 — o botão "+" gravava o número errado e o
 * alerta de estoque mínimo comparava valores falsos.
 */
class QuantityParsingTest {

    private val delta = 0.0001

    @Test
    fun numeroInteiroViraDouble() {
        // Regressão: "1" (quantidade padrão dos formulários) precisa continuar valendo 1.0.
        assertEquals(1.0, CurrencyUtils.parseQuantity("1"), delta)
    }

    @Test
    fun virgulaDecimalEhLidaComoFracao() {
        // Regressão CRÍTICA: "1,5" era lido como 1.0; agora precisa valer 1.5.
        assertEquals(1.5, CurrencyUtils.parseQuantity("1,5"), delta)
    }

    @Test
    fun pontoDecimalContinuaFuncionando() {
        // Regressão: normalizar a vírgula não pode quebrar quem digita/cola "1.5".
        assertEquals(1.5, CurrencyUtils.parseQuantity("1.5"), delta)
    }

    @Test
    fun espacosEmVoltaSaoIgnorados() {
        // Regressão: campo com espaço (" 2 ") virava null e caía no fallback 1.0.
        assertEquals(2.0, CurrencyUtils.parseQuantity(" 2 "), delta)
    }

    @Test
    fun textoVazioUsaOFallbackPadrao() {
        // Regressão: campo vazio deve valer 1.0 (padrão), não 0.0 nem exceção.
        assertEquals(1.0, CurrencyUtils.parseQuantity(""), delta)
    }

    @Test
    fun textoInvalidoUsaOFallbackPadrao() {
        // Regressão: digitação sem número não pode derrubar o app nem gravar lixo.
        assertEquals(1.0, CurrencyUtils.parseQuantity("abc"), delta)
    }

    @Test
    fun virgulaVenceOFallbackZero() {
        // Regressão: com fallback 0.0 o valor lido ("1,5") ainda tem de ganhar do fallback.
        assertEquals(1.5, CurrencyUtils.parseQuantity("1,5", fallback = 0.0), delta)
    }

    @Test
    fun vazioComFallbackZeroRetornaZero() {
        // Regressão: quem pede fallback 0.0 (ex.: soma de estoque) precisa receber 0.0.
        assertEquals(0.0, CurrencyUtils.parseQuantity("", fallback = 0.0), delta)
    }

    @Test
    fun nanNaoEhAceitoComoQuantidade() {
        // Regressão: Double.parseDouble("NaN") não estoura, mas contamina toda soma
        // (NaN se propaga); precisa cair no fallback.
        assertEquals(1.0, CurrencyUtils.parseQuantity("NaN"), delta)
    }

    @Test
    fun infinitoNaoEhAceitoComoQuantidade() {
        // Regressão: "Infinity" passava pelo toDoubleOrNull e virava estoque infinito.
        assertEquals(1.0, CurrencyUtils.parseQuantity("Infinity"), delta)
    }

    @Test
    fun zeroEhQuantidadeValida() {
        // Regressão: 0 é um número válido digitado pelo usuário e não pode virar 1.0.
        assertEquals(0.0, CurrencyUtils.parseQuantity("0"), delta)
    }

    @Test
    fun quantidadeGrandeEhPreservada() {
        // Regressão: valores altos (gramas) não podem ser truncados na conversão.
        assertEquals(500.0, CurrencyUtils.parseQuantity("500"), delta)
    }

    @Test
    fun totalEmQuilosAceitaVirgula() {
        // Regressão: 1,5 kg a R$ 10,00 tem de dar R$ 15,00 (antes dava R$ 10,00).
        assertEquals(15.0, CurrencyUtils.calculateItemTotal("1,5", "kg", 10.0), delta)
    }

    @Test
    fun totalEmGramasConverteParaQuilo() {
        // Regressão: a conversão g -> kg precisa continuar valendo após a mudança do parser.
        assertEquals(20.0, CurrencyUtils.calculateItemTotal("500", "g", 40.0), delta)
    }

    // ─────────────── Escrever a quantidade de volta ───────────────
    // Usado pelo "Usei 1" da pergunta do dia: 3 → 2 tem de gravar "2", não "2.0".

    @Test
    fun quantidadeInteiraEhEscritaSemCasas() {
        assertEquals("2", CurrencyUtils.formatarQuantidade(2.0))
    }

    @Test
    fun quantidadeQuebradaEhEscritaComVirgula() {
        // 1,5 kg depois de usar 1 vira 0,5 — e o app fala português.
        assertEquals("0,5", CurrencyUtils.formatarQuantidade(0.5))
    }

    @Test
    fun quantidadeZeroEhEscritaComoZero() {
        assertEquals("0", CurrencyUtils.formatarQuantidade(0.0))
    }

    @Test
    fun fracaoDeDuasCasasEhPreservada() {
        assertEquals("2,25", CurrencyUtils.formatarQuantidade(2.25))
    }
}
