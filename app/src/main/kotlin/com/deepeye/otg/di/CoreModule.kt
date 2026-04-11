package com.deepeye.otg.di

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.room.Room
import com.deepeye.otg.data.DeepEyeDatabase
import com.deepeye.otg.data.db.AppDatabase
import com.deepeye.otg.engine.RamdiskForensicEngine
import com.deepeye.otg.exploit.ExploitExecutor
import com.deepeye.otg.intelligence.vulndb.CveDao
import com.deepeye.otg.intelligence.vulndb.CveDatabase
import com.deepeye.otg.intelligence.vulndb.CveImporter
import com.deepeye.otg.usb.AdbExecutor
import com.deepeye.otg.usb.AdbManager
import com.deepeye.otg.usb.HardwareManager
import com.deepeye.otg.usb.SessionCoordinator
import com.deepeye.otg.usb.UsbLifecycleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideApplicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    @Provides
    @Singleton
    fun provideSessionCoordinator(): SessionCoordinator {
        return SessionCoordinator()
    }

    @Provides
    @Singleton
    fun provideUsbLifecycleManager(
        @ApplicationContext context: Context,
        usbManager: UsbManager,
        scope: CoroutineScope,
        coordinator: SessionCoordinator
    ): UsbLifecycleManager {
        return UsbLifecycleManager(context, usbManager, scope, coordinator)
    }

    @Provides
    @Singleton
    fun provideAdbManager(lifecycleManager: UsbLifecycleManager): AdbManager {
        return AdbManager(lifecycleManager)
    }

    @Provides
    @Singleton
    fun provideHardwareManager(
        @ApplicationContext context: Context,
        lifecycleManager: UsbLifecycleManager
    ): HardwareManager {
        return HardwareManager(context, lifecycleManager)
    }

    @Provides
    @Singleton
    fun provideAdbExecutor(): AdbExecutor {
        return AdbExecutor()
    }

    @Provides
    @Singleton
    fun provideExploitExecutor(
        @ApplicationContext context: Context,
        adbExecutor: AdbExecutor
    ): ExploitExecutor {
        return ExploitExecutor(context, adbExecutor)
    }

    @Provides
    @Singleton
    fun provideRamdiskForensicEngine(): RamdiskForensicEngine {
        return RamdiskForensicEngine()
    }

    @Provides
    @Singleton
    fun provideCveDatabase(@ApplicationContext context: Context): CveDatabase {
        return CveDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCveDao(database: CveDatabase): CveDao {
        return database.cveDao()
    }

    @Provides
    @Singleton
    fun provideCveImporter(cveDao: CveDao): CveImporter {
        return CveImporter(cveDao)
    }

    @Provides
    @Singleton
    fun provideDeepEyeDatabase(@ApplicationContext context: Context): DeepEyeDatabase {
        return Room.databaseBuilder(
            context,
            DeepEyeDatabase::class.java,
            "deepeye-db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
