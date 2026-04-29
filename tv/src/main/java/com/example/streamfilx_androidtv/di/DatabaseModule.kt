package com.example.streamfilx_androidtv.di

import android.content.Context
import androidx.room.Room
import com.example.streamfilx_androidtv.data.db.StreamFlixDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StreamFlixDatabase =
        Room.databaseBuilder(
            context,
            StreamFlixDatabase::class.java,
            "streamflix.db",
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProfileDao(db: StreamFlixDatabase) = db.profileDao()

    @Provides
    fun provideFavoritesDao(db: StreamFlixDatabase) = db.favoritesDao()

    @Provides
    fun provideWatchHistoryDao(db: StreamFlixDatabase) = db.watchHistoryDao()

    @Provides
    fun provideDownloadDao(db: StreamFlixDatabase) = db.downloadDao()
}
