package com.example.stockup.domain

import java.text.Normalizer

/**
 * Compara o que a pessoa digitou com o nome do produto.
 *
 * Por que existe: o teclado do celular em português quase nunca põe acento — e
 * quem procura "classico" espera achar "Clássico". Antes a busca usava
 * `contains(ignoreCase = true)`, que resolvia maiúscula mas não acento, e um
 * espaço sobrando no fim (muito fácil de digitar) zerava o resultado.
 *
 * A normalização tira acento, caixa e espaço das DUAS pontas antes de comparar.
 */
object BuscaDeProduto {

    /** Minúsculo, sem acento e com espaços colapsados. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")

    /**
     * O produto combina com a consulta?
     *
     * Consulta em branco combina com tudo (é o estado normal da lista).
     * Também procura no **local** ("armário") e na **categoria** ("bebidas"), porque
     * é assim que a pessoa lembra do produto quando esqueceu o nome exato.
     */
    fun combina(nome: String, local: String, categoria: String, consulta: String): Boolean {
        val alvo = normalizar(consulta)
        if (alvo.isBlank()) return true
        return normalizar(nome).contains(alvo) ||
            normalizar(local).contains(alvo) ||
            normalizar(categoria).contains(alvo)
    }
}
