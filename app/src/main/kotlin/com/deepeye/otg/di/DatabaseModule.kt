package com.deepeye.otg.di

import android.content.Context
import androidx.room.Room
import com.deepeye.otg.data.db.AppDatabase
import com.deepeye.otg.data.db.dao.ForensicDao
import com.deepeye.otg.data.db.dao.FuzzDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "deepeye_forensics.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideForensicDao(db: AppDatabase): ForensicDao {
        return db.forensicDao()
    }

    @Provides
    @Singleton
    fun provideFuzzDao(db: AppDatabase): FuzzDao {
        return db.fuzzDao()
    }
}
