package fr.geoking.arthur.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArtworkCacheEntity::class,
        InvalidArtworkEntity::class,
        CustomFractalEntity::class,
        QuoteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ArthurDatabase : RoomDatabase() {
    abstract fun artworkCacheDao(): ArtworkCacheDao
    abstract fun invalidArtworkDao(): InvalidArtworkDao
    abstract fun customFractalDao(): CustomFractalDao
    abstract fun quoteDao(): QuoteDao
}
