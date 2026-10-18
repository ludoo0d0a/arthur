package fr.geoking.arthur.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ArtworkCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<ArtworkCacheEntity>)

    @Query("SELECT * FROM artwork_cache WHERE id = :id")
    suspend fun getById(id: String): ArtworkCacheEntity?

    @Query("SELECT * FROM artwork_cache")
    suspend fun getAll(): List<ArtworkCacheEntity>

    @Query("SELECT COUNT(*) FROM artwork_cache")
    suspend fun count(): Int

    @Query("SELECT * FROM artwork_cache")
    fun observeAll(): Flow<List<ArtworkCacheEntity>>

    @Query("SELECT * FROM artwork_cache WHERE LOWER(category) = LOWER(:category) AND sourceId = :sourceId")
    suspend fun getByCategoryAndSource(category: String, sourceId: String): List<ArtworkCacheEntity>

    @Query("SELECT * FROM artwork_cache WHERE category = 'genart' ORDER BY lastAccess DESC LIMIT :limit")
    suspend fun getGenartStills(limit: Int): List<ArtworkCacheEntity>

    @Query("SELECT * FROM artwork_cache WHERE category = 'genart' ORDER BY lastAccess ASC")
    suspend fun getAllGenartLru(): List<ArtworkCacheEntity>

    @Query("DELETE FROM artwork_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM artwork_cache")
    suspend fun deleteAll()
}
