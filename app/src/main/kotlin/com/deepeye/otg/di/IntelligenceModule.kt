package com.deepeye.otg.di

import android.content.Context
import com.deepeye.otg.intelligence.vulndb.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntelligenceModule {

    @Provides
    @Singleton
    fun provideCveDatabase(@ApplicationContext context: Context): CveDatabase {
        return CveDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCveDao(db: CveDatabase): CveDao {
        return db.cveDao()
    }

    @Provides
    @Singleton
    fun provideCveImporter(cveDao: CveDao): CveImporter {
        return CveImporter(cveDao)
    }

    @Provides
    @Singleton
    fun provideVersionMappingEngine(): VersionMappingEngine {
        return VersionMappingEngine()
    }

    @Provides
    @Singleton
    fun providePatchStateAnalyzer(cveDao: CveDao): PatchStateAnalyzer {
        return PatchStateAnalyzer(cveDao)
    }

    @Provides
    @Singleton
    fun provideCveRepository(
        cveDao: CveDao,
        importer: CveImporter,
        analyzer: PatchStateAnalyzer
    ): CveRepository {
        return CveRepository(cveDao, importer, analyzer)
    }

    @Provides
    @Singleton
    fun provideRamdiskForensicEngine(): com.deepeye.otg.engine.RamdiskForensicEngine {
        return com.deepeye.otg.engine.RamdiskForensicEngine()
    }
}
