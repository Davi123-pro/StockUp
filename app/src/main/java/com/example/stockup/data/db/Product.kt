package com.example.stockup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val photoUri: String? = null,
    val quantity: String = "1",
    val unit: String = "un", // un, kg, L, g
    val expirationDate: Long = 0L, // timestamp em milissegundos (0 se sem validade)
    val category: String = "Despensa", // Laticínios, Bebidas, Despensa, Limpeza, etc.
    val notes: String = "",
    val price: Double = 0.0,
    val location: String = "Geral", // Geladeira, Armário, Loja, etc.
    val createdAt: Long = System.currentTimeMillis(),
    val hasExpiration: Boolean = true,
    val minStock: Double = 0.0, // 0.0 = desligado; > 0 = alerta quando estoque <= minStock
    val barcode: String? = null
)
