package fr.geoking.arthur.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invalid_artwork")
data class InvalidArtworkEntity(
    @PrimaryKey val artworkId: String,
    val timestamp: Long,
)
