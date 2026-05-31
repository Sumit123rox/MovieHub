package com.moviehub.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters

@Database(
    entities = [
        AddonEntity::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class,
        UserPreferencesEntity::class,
        WatchHistoryEntity::class,
        WatchProgress::class,
        StremioCacheEntity::class,
        ProfileEntity::class,
        DownloadEntity::class,
        CustomCollectionEntity::class,
        CollectionItemEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
@TypeConverters(Converters::class)
@ConstructedBy(MovieDatabaseConstructor::class)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun addonDao(): AddonDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun stremioCacheDao(): StremioCacheDao
    abstract fun profileDao(): ProfileDao
    abstract fun downloadDao(): DownloadDao
    abstract fun customCollectionDao(): CustomCollectionDao
}

expect object MovieDatabaseConstructor : RoomDatabaseConstructor<MovieDatabase>
