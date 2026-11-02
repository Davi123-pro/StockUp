package com.example.stockup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumoDao {

    // ─────────────── observações de consumo ───────────────

    @Query("SELECT * FROM consumo_observacoes WHERE chave = :chave ORDER BY quando DESC")
    suspend fun observacoesDe(chave: String): List<ConsumoObservacao>

    @Insert
    suspend fun inserirObservacao(observacao: ConsumoObservacao): Long

    /** Remove a observação mais recente — usado pelo "Desfazer". */
    @Query(
        "DELETE FROM consumo_observacoes WHERE id = (" +
            "SELECT id FROM consumo_observacoes WHERE chave = :chave ORDER BY quando DESC LIMIT 1)"
    )
    suspend fun removerUltimaObservacao(chave: String)

    /** Mantém só as [limite] mais recentes por produto (as antigas não ajudam a prever). */
    @Query(
        "DELETE FROM consumo_observacoes WHERE chave = :chave AND id NOT IN (" +
            "SELECT id FROM consumo_observacoes WHERE chave = :chave ORDER BY quando DESC LIMIT :limite)"
    )
    suspend fun podarObservacoes(chave: String, limite: Int)

    /** Tudo o que já foi consumido — para telas futuras e para depuração. */
    @Query("SELECT * FROM consumo_observacoes ORDER BY quando DESC")
    fun todasObservacoes(): Flow<List<ConsumoObservacao>>

    // ─────────────── histórico de estoque ───────────────

    @Insert
    suspend fun inserirAtualizacao(atualizacao: EstoqueAtualizacao): Long

    @Query("SELECT MAX(quando) FROM estoque_atualizacoes WHERE chave = :chave")
    suspend fun ultimaAtualizacaoDe(chave: String): Long?

    @Query("SELECT * FROM estoque_atualizacoes WHERE chave = :chave ORDER BY quando DESC")
    suspend fun atualizacoesDe(chave: String): List<EstoqueAtualizacao>

    // ─────────────── agenda de avisos ───────────────

    @Query("SELECT * FROM avisos_consumo WHERE chave = :chave")
    suspend fun avisoDe(chave: String): AvisoConsumo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarAviso(aviso: AvisoConsumo)

    @Query("SELECT * FROM avisos_consumo")
    suspend fun todosAvisos(): List<AvisoConsumo>

    @Query("DELETE FROM avisos_consumo WHERE chave = :chave")
    suspend fun apagarAviso(chave: String)

    // ─────────────── limpeza ───────────────

    @Query("DELETE FROM consumo_observacoes WHERE chave = :chave")
    suspend fun apagarObservacoes(chave: String)

    @Query("DELETE FROM estoque_atualizacoes WHERE chave = :chave")
    suspend fun apagarAtualizacoes(chave: String)
}
