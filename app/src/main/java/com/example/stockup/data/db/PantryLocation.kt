package com.example.stockup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_locations")
data class PantryLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String
)
