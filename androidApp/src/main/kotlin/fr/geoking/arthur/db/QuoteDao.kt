package fr.geoking.arthur.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes WHERE providerId = :providerId ORDER BY id ASC")
    suspend fun getQuotesForProvider(providerId: String): List<QuoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)

    @Query("DELETE FROM quotes WHERE providerId = :providerId")
    suspend fun deleteForProvider(providerId: String)

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()
}
