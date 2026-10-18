package fr.geoking.arthur.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_fractal")
data class CustomFractalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
)
