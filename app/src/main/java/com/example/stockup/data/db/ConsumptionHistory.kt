package com.example.stockup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumption_history")
data class ConsumptionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productName: String,
    val quantity: String,
    val price: Double,
    val wasWasted: Boolean, // True = Venceu/Jogado fora. False = Consumido.
    val category: String = "Despensa",
    val timestamp: Long = System.currentTimeMillis()
)
