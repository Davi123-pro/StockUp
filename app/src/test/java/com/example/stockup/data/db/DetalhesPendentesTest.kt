package com.example.stockup.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guarda o bug do "Detalhes pendentes" que o usuário relatou: o aviso continuava
 * aparecendo depois de ele editar o produto, porque o marcador era procurado com
 * um `contains("Pendente")` solto e nunca era removido das anotações.
 */
class DetalhesPendentesTest {

    @Test
    fun `produto vindo do mover rapido esta pendente`() {
        val p = Product(name = "Leite", notes = MARCADOR_DETALHES_PENDENTES)
        assertTrue(p.temDetalhesPendentes())
    }

    @Test
    fun `marcador junto de outras anotacoes ainda conta`() {
        val p = Product(
            name = "Leite",
            notes = "$MARCADOR_DETALHES_PENDENTES comprar sempre a marca Cemil"
        )
        assertTrue(p.temDetalhesPendentes())
    }

    @Test
    fun `produto comum nao esta pendente`() {
        val p = Product(name = "Arroz", notes = "")
        assertFalse(p.temDetalhesPendentes())
    }

    @Test
    fun `a palavra pendente solta nao cria aviso falso`() {
        // Antes, escrever "pendente" nas anotações fazia o app mostrar o selo.
        val p = Product(name = "Arroz", notes = "comprar pendente de promoção")
        assertFalse(p.temDetalhesPendentes())
    }

    @Test
    fun `nota com caixa diferente nao escapa`() {
        val p = Product(name = "Leite", notes = "⚠️ pendente DETALHES (vindo de compras)")
        assertTrue(p.temDetalhesPendentes())
    }
}
