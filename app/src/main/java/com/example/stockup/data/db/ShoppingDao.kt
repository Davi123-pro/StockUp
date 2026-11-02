package com.example.stockup.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    /** Leitura pontual, para decidir se o produto já está na lista antes de somar. */
    @Query("SELECT * FROM shopping_items")
    suspend fun getAllItemsOnce(): List<ShoppingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isCompleted = 1")
    suspend fun deleteCompletedItems()

    /** Usado só pela restauração de cópia de segurança, que substitui tudo. */
    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllItems()
}
