package com.example.stockup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM consumption_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConsumptionHistory>>

    @Query("SELECT * FROM consumption_history WHERE timestamp >= :startTimestamp ORDER BY timestamp DESC")
    fun getHistorySince(startTimestamp: Long): Flow<List<ConsumptionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ConsumptionHistory): Long

    /** Usado pelo "Desfazer" logo após apagar um produto. */
    @Query("DELETE FROM consumption_history WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    /**
     * Corrige um consumo já registrado: "na verdade eu joguei fora".
     *
     * É o que permite o caminho principal ser UM toque só. Em vez de perguntar
     * "usou ou jogou fora?" antes de apagar, o app assume "usei" e deixa a correção
     * na barra de aviso, que fica disponível por alguns segundos.
     */
    @Query("UPDATE consumption_history SET wasWasted = 1 WHERE id IN (:ids)")
    suspend fun marcarComoDesperdicio(ids: List<Long>)

    @Query("DELETE FROM consumption_history")
    suspend fun clearHistory()
}
