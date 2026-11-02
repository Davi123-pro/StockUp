package com.example.stockup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val quantity: String = "1",
    val unit: String = "un",
    val category: String = "Despensa",
    val price: Double? = null,
    val isCompleted: Boolean = false,
    val isUrgent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
