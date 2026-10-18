package fr.geoking.arthur.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artwork_cache")
data class ArtworkCacheEntity(
    @PrimaryKey val id: String,
    val title: String,
    val attribution: String,
    val sourceId: String,
    val kind: String,
    val remoteUrl: String?,
    val category: String,
    val lastAccess: Long,
)
