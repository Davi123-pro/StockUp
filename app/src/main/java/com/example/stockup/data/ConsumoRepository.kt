package com.example.stockup.data

import com.example.stockup.data.db.AvisoConsumo
import com.example.stockup.data.db.ConsumoDao
import com.example.stockup.data.db.ConsumoObservacao
import com.example.stockup.data.db.EstoqueAtualizacao
import com.example.stockup.domain.ObservacaoConsumo

/**
 * O histórico que alimenta a estimativa de consumo.
 *
 * Antes isto era um JSON em `filesDir`; agora é tabela. A API é a mesma de antes
 * (`registrarConsumo`, `observacoes`, `aviso`, `registrarAviso`, `registrarResposta`,
 * `removerUltimaObservacao`) para os pontos de chamada não precisarem saber onde o
 * dado mora.
 */
class ConsumoRepository(private val dao: ConsumoDao) {

    companion object {
        const val RESPOSTA_AINDA_TENHO = "ainda_tenho"
        const val RESPOSTA_ACABANDO = "acabando"
        const val RESPOSTA_ACABOU = "acabou"
        const val RESPOSTA_MAIS_TARDE = "mais_tarde"

        /** Quantas observações entram na média (as mais recentes). */
        private const val MAX_POR_PRODUTO = 10
    }

    /** Observações do produto, já no formato que o motor entende. */
    suspend fun observacoes(chave: String): List<ObservacaoConsumo> =
        dao.observacoesDe(chave).map {
            ObservacaoConsumo(
                chave = it.chave,
                quantidade = it.quantidade,
                dias = it.dias,
                quando = it.quando
            )
        }

    /**
     * Registra consumo e o histórico de estoque.
     *
     * @param quantidade quanto foi consumido
     * @param dias em quantos dias isso aconteceu
     * @param origem "acabou", "manual" ou "aviso"
     */
    suspend fun registrarConsumo(
        chave: String,
        quantidade: Double,
        dias: Double,
        origem: String = "acabou",
        quando: Long = System.currentTimeMillis()
    ) {
        dao.inserirObservacao(
            ConsumoObservacao(
                chave = chave,
                quantidade = quantidade,
                dias = if (dias > 0.0) dias else 0.5,
                quando = quando
            )
        )
        dao.inserirAtualizacao(
            EstoqueAtualizacao(
                chave = chave,
                quantidadeAnterior = quantidade,
                quantidadeAtual = 0.0,
                quando = quando,
                origem = origem
            )
        )
        dao.podarObservacoes(chave, MAX_POR_PRODUTO)
    }

    /** Registra só a mudança de estoque (sem entrada na média). */
    suspend fun registrarAtualizacaoDeEstoque(
        chave: String,
        anterior: Double,
        atual: Double,
        origem: String,
        quando: Long = System.currentTimeMillis()
    ) {
        dao.inserirAtualizacao(
            EstoqueAtualizacao(
                chave = chave,
                quantidadeAnterior = anterior,
                quantidadeAtual = atual,
                quando = quando,
                origem = origem
            )
        )
    }

    /** Última vez que o estoque desta chave mudou (base do "há quantos dias"). */
    suspend fun ultimaAtualizacao(chave: String): Long = dao.ultimaAtualizacaoDe(chave) ?: 0L

    suspend fun removerUltimaObservacao(chave: String) {
        dao.removerUltimaObservacao(chave)
    }

    suspend fun aviso(chave: String): AvisoConsumo = dao.avisoDe(chave) ?: AvisoConsumo(chave = chave)

    /** Registra que perguntamos. Silêncio anterior conta como aviso ignorado. */
    suspend fun registrarAviso(chave: String, proximo: Long, quando: Long = System.currentTimeMillis()) {
        val atual = dao.avisoDe(chave) ?: AvisoConsumo(chave = chave)
        val semResposta = atual.ultimoAviso > 0L && atual.ultimaResposta.isBlank()
        dao.salvarAviso(
            atual.copy(
                ultimoAviso = quando,
                proximoAviso = proximo,
                ignorados = if (semResposta) atual.ignorados + 1 else atual.ignorados,
                ultimaResposta = ""
            )
        )
    }

    /** Guarda a resposta. Só "mais tarde" conta como silêncio. */
    suspend fun registrarResposta(chave: String, resposta: String, proximo: Long) {
        val atual = dao.avisoDe(chave) ?: AvisoConsumo(chave = chave)
        dao.salvarAviso(
            atual.copy(
                ultimaResposta = resposta,
                ignorados = if (resposta == RESPOSTA_MAIS_TARDE) atual.ignorados + 1 else 0,
                proximoAviso = proximo
            )
        )
    }

    /** Esquece o produto (quando ele é apagado de vez da despensa). */
    suspend fun esquecer(chave: String) {
        dao.apagarObservacoes(chave)
        dao.apagarAtualizacoes(chave)
        dao.apagarAviso(chave)
    }
}
