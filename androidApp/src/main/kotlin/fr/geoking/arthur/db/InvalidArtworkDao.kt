package fr.geoking.arthur.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvalidArtworkDao {
    @Query("SELECT EXISTS(SELECT 1 FROM invalid_artwork WHERE artworkId = :artworkId)")
    suspend fun isInvalid(artworkId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markInvalid(entity: InvalidArtworkEntity)

    @Query("DELETE FROM invalid_artwork WHERE artworkId = :artworkId")
    suspend fun removeInvalid(artworkId: String)

    @Query("SELECT artworkId FROM invalid_artwork ORDER BY timestamp ASC")
    suspend fun getAllIds(): List<String>

    @Query("SELECT artworkId FROM invalid_artwork ORDER BY timestamp ASC")
    fun observeInvalidIds(): Flow<List<String>>

    @Query("DELETE FROM invalid_artwork WHERE artworkId NOT IN (SELECT artworkId FROM invalid_artwork ORDER BY timestamp DESC LIMIT :maxKeep)")
    suspend fun trimOldest(maxKeep: Int)

    @Query("DELETE FROM invalid_artwork")
    suspend fun deleteAll()
}
