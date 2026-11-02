package com.example.stockup.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM pantry_locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<PantryLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: PantryLocation): Long

    @Delete
    suspend fun deleteLocation(location: PantryLocation)

    /** Usado só pela restauração de cópia de segurança, que substitui tudo. */
    @Query("DELETE FROM pantry_locations")
    suspend fun deleteAllLocations()
}
