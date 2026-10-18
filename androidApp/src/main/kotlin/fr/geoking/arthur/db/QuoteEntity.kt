package fr.geoking.arthur.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String,
    val providerId: String,
    val cachedAtMs: Long,
)
