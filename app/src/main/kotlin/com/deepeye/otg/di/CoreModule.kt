package com.deepeye.otg.di

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.room.Room
import com.deepeye.otg.data.DeepEyeDatabase
import com.deepeye.otg.data.db.AppDatabase
import com.deepeye.otg.data.db.dao.ForensicDao
import com.deepeye.otg.data.db.dao.FuzzDao
import com.deepeye.otg.data.tauri.RealTauriBridge
import com.deepeye.otg.data.tauri.TauriBridge
import com.deepeye.otg.engine.CloudVaultManager
import com.deepeye.otg.engine.RamdiskForensicEngine
import com.deepeye.otg.engine.TokenManager
import com.deepeye.otg.engine.mtk.MtkExploitEngine
import com.deepeye.otg.engine.xiaomi.XiaomiExploitEngine
import com.deepeye.otg.exploit.ExploitExecutor
import com.deepeye.otg.intelligence.vulndb.CveDao
import com.deepeye.otg.intelligence.vulndb.CveDatabase
import com.deepeye.otg.intelligence.vulndb.CveImporter
import com.deepeye.otg.intelligence.vulndb.PatchStateAnalyzer
import com.deepeye.otg.protocol.apple.UsbAppleSession
import com.deepeye.otg.usb.AdbExecutor
import com.deepeye.otg.usb.AdbManager
import com.deepeye.otg.usb.AdbSession
import com.deepeye.otg.usb.HardwareManager
import com.deepeye.otg.usb.MtkAuthHandler
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

    // Provide plain Context (Hilt needs this explicitly for classes not using @ApplicationContext)
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
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
    fun provideTokenManager(): TokenManager {
        return TokenManager()
    }

    @Provides
    @Singleton
    fun provideMtkAuthHandler(): MtkAuthHandler {
        return MtkAuthHandler()
    }

    @Provides
    @Singleton
    fun provideAdbSession(): AdbSession {
        return AdbSession() // Transport will be set via initialize() when device connects
    }

    // Provide real TauriBridge implementation for Apple device operations
    @Provides
    @Singleton
    fun provideTauriBridge(
        @ApplicationContext context: Context,
        lifecycleManager: UsbLifecycleManager
    ): TauriBridge {
        return RealTauriBridge(context, lifecycleManager)
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

    @Provides
    @Singleton
    fun provideFuzzDao(database: AppDatabase): FuzzDao {
        return database.fuzzDao()
    }

    @Provides
    @Singleton
    fun provideForensicDao(database: AppDatabase): ForensicDao {
        return database.forensicDao()
    }

    // ══════════════════════════════════════════
    // EXPLOIT ENGINES (MTK + Xiaomi)
    // ══════════════════════════════════════════

    @Provides
    @Singleton
    fun provideMtkExploitEngine(
        @ApplicationContext context: Context
    ): MtkExploitEngine = MtkExploitEngine(context)

    @Provides
    @Singleton
    fun provideXiaomiExploitEngine(
        @ApplicationContext context: Context
    ): XiaomiExploitEngine = XiaomiExploitEngine(context)

    @Provides
    @Singleton
    fun provideUsbAppleSession(usbManager: UsbManager): UsbAppleSession {
        return UsbAppleSession(usbManager)
    }

    @Provides
    @Singleton
    fun providePatchStateAnalyzer(cveDao: CveDao): PatchStateAnalyzer {
        return PatchStateAnalyzer(cveDao)
    }

    @Provides
    @Singleton
    fun provideFridaManager(
        @ApplicationContext context: Context,
        adbExecutor: AdbExecutor
    ): com.deepeye.otg.intelligence.FridaManager {
        return com.deepeye.otg.intelligence.FridaManager(context, adbExecutor)
    }
}
