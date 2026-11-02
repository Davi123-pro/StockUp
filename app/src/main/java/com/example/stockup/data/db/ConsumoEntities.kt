package com.example.stockup.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Uma vez que o produto foi consumido.
 *
 * Antes isto vivia num arquivo JSON (`consumo.json`), porque o dono tinha pedido
 * para não tocar em migrations. Em 02/11 ele liberou as migrations — então o
 * histórico veio para o banco, onde sempre deveria estar: transação de verdade,
 * consulta de verdade e sem risco de arquivo corrompido.
 */
@Entity(
    tableName = "consumo_observacoes",
    indices = [Index("chave"), Index("quando")]
)
data class ConsumoObservacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Nome normalizado + unidade. Identifica o produto entre uma compra e outra. */
    val chave: String,
    /** Quanto foi consumido, na unidade do produto. */
    val quantidade: Double,
    /** Em quantos dias isso aconteceu. */
    val dias: Double,
    /** Quando foi registrado (ms). */
    val quando: Long
)

/**
 * Uma mudança de estoque: quanto tinha, quanto passou a ter, quando e por quê.
 *
 * É o "histórico de estoque" pedido na especificação. Serve para auditar a
 * estimativa ("de onde veio esse número?") e para o futuro calcular consumo por
 * diferença, sem depender só do "acabou".
 */
@Entity(
    tableName = "estoque_atualizacoes",
    indices = [Index("chave"), Index("quando")]
)
data class EstoqueAtualizacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val chave: String,
    val quantidadeAnterior: Double,
    val quantidadeAtual: Double,
    val quando: Long,
    /** "manual", "aviso" (respondido na notificação) ou "acabou". */
    val origem: String
)

/**
 * A agenda de avisos de um produto: quando foi o último, quantos foram ignorados
 * e quando deve voltar.
 *
 * A chave é o próprio identificador do produto (nome normalizado + unidade), então
 * não existe mais de uma linha por produto.
 */
@Entity(tableName = "avisos_consumo")
data class AvisoConsumo(
    @PrimaryKey
    val chave: String,
    val ultimoAviso: Long = 0L,
    val ignorados: Int = 0,
    val ultimaResposta: String = "",
    val proximoAviso: Long = 0L
)
