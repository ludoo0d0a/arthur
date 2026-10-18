package fr.geoking.arthur.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFractalDao {
    @Query("SELECT * FROM custom_fractal ORDER BY createdAt DESC")
    suspend fun getAll(): List<CustomFractalEntity>

    @Query("SELECT * FROM custom_fractal ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomFractalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CustomFractalEntity)

    @Query("DELETE FROM custom_fractal WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_fractal")
    suspend fun deleteAll()
}
