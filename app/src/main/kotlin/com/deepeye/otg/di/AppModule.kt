package com.deepeye.otg.di

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import com.deepeye.otg.DeepEyeApplication
import com.deepeye.otg.data.SettingsManager
import com.deepeye.otg.engine.ForensicAiAssistant
import com.deepeye.otg.engine.ForensicEngine
import com.deepeye.otg.intelligence.vulndb.*
import com.deepeye.otg.service.MassExtractor
import androidx.room.Room
import com.deepeye.otg.data.db.AppDatabase
import com.deepeye.otg.engine.RamdiskForensicEngine
import com.deepeye.otg.usb.AdbManager
import com.deepeye.otg.usb.AdbSession
import com.deepeye.otg.usb.BulkTransport
import com.deepeye.otg.usb.HardwareManager
import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbDescriptorSnapshot
import com.deepeye.otg.usb.UsbTransport
import com.deepeye.otg.usb.SessionCoordinator
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.usb.IosSessionCoordinator
import com.deepeye.otg.fuzz.hid.HidFuzzCoordinator
import com.deepeye.otg.fuzz.hid.MaliciousHidDevice
import com.deepeye.otg.exploit.ExploitChainOrchestrator
import com.deepeye.otg.exploit.PostExploitExtractor
import com.deepeye.otg.exploit.AslrDefeater
import com.deepeye.otg.exploit.UniversalExploitOrchestrator
import com.deepeye.otg.exploit.AmfiSymlinkBypass
import com.deepeye.otg.data.tauri.NoOpTauriBridge
import com.deepeye.otg.data.tauri.TauriBridge
import com.deepeye.otg.data.db.dao.FuzzDao
import com.deepeye.otg.data.db.dao.ForensicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSessionCoordinator(): SessionCoordinator {
        return SessionCoordinator()
    }

    @Provides
    @Singleton
    fun provideHidFuzzCoordinator(
        lifecycleManager: UsbLifecycleManager,
        sessionCoordinator: SessionCoordinator,
        fuzzDao: FuzzDao
    ): HidFuzzCoordinator {
        return HidFuzzCoordinator(lifecycleManager, sessionCoordinator, fuzzDao)
    }

    @Provides
    @Singleton
    fun providePostExploitExtractor(): PostExploitExtractor {
        return PostExploitExtractor()
    }

    @Provides
    @Singleton
    fun provideAslrDefeater(): AslrDefeater {
        return AslrDefeater()
    }

    @Provides
    @Singleton
    fun provideAmfiSymlinkBypass(): AmfiSymlinkBypass {
        return AmfiSymlinkBypass()
    }

    @Provides
    @Singleton
    fun provideMaliciousHidDevice(): MaliciousHidDevice {
        return MaliciousHidDevice()
    }

    @Provides
    @Singleton
    fun provideExploitChainOrchestrator(
        extractor: PostExploitExtractor,
        aslrDefeater: AslrDefeater,
        amfiBypass: AmfiSymlinkBypass,
        hidDevice: MaliciousHidDevice,
        fuzzDao: FuzzDao,
        sessionCoordinator: SessionCoordinator
    ): ExploitChainOrchestrator {
        return ExploitChainOrchestrator(extractor, aslrDefeater, amfiBypass, hidDevice, fuzzDao, sessionCoordinator)
    }

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    @Provides
    @Singleton
    fun provideTauriBridge(noOpTauriBridge: NoOpTauriBridge): TauriBridge {
        return noOpTauriBridge
    }

    // FirmwareAssetManager is now provided via its @Inject constructor and @Singleton annotation.

    @Provides
    @Singleton
    fun provideAppScope(application: Application): CoroutineScope {
        return (application as com.deepeye.otg.DeepEyeApplication).appScope
    }

    @Provides
    @Singleton
    fun provideJailbreakEngine(
        usbLifecycleManager: UsbLifecycleManager
    ): com.deepeye.otg.engine.JailbreakEngine {
        return com.deepeye.otg.engine.JailbreakEngine(usbLifecycleManager)
    }

    @Provides
    @Singleton
    fun providePurpleEngine(
        usbLifecycleManager: UsbLifecycleManager
    ): com.deepeye.otg.engine.PurpleEngine {
        return com.deepeye.otg.engine.PurpleEngine(usbLifecycleManager)
    }

    @Provides
    @Singleton
    fun provideTokenManager(): com.deepeye.otg.engine.TokenManager {
        return com.deepeye.otg.engine.TokenManager()
    }

    @Provides
    @Singleton
    fun provideCloudVaultManager(): com.deepeye.otg.engine.CloudVaultManager {
        return com.deepeye.otg.engine.CloudVaultManager()
    }

    @Provides
    @Singleton
    fun provideMtkEngine(
        usbLifecycleManager: com.deepeye.otg.usb.UsbLifecycleManager
    ): com.deepeye.otg.engine.MtkEngine {
        return com.deepeye.otg.engine.MtkEngine(usbLifecycleManager)
    }

    @Provides
    @Singleton
    fun provideActivationEngine(
        usbManager: android.hardware.usb.UsbManager,
        jailbreakEngine: com.deepeye.otg.engine.JailbreakEngine,
        purpleEngine: com.deepeye.otg.engine.PurpleEngine,
        tokenManager: com.deepeye.otg.engine.TokenManager,
        cloudVaultManager: com.deepeye.otg.engine.CloudVaultManager,
        cveDatabase: com.deepeye.otg.intelligence.vulndb.CveDatabase
    ): com.deepeye.otg.engine.ActivationEngine {
        return com.deepeye.otg.engine.ActivationEngine(
            usbManager, 
            jailbreakEngine, 
            purpleEngine, 
            tokenManager,
            cloudVaultManager,
            cveDatabase
        )
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideForensicEngine(@ApplicationContext context: Context): ForensicEngine {
        return ForensicEngine(context)
    }

    @Provides
    @Singleton
    fun provideForensicAiAssistant(): ForensicAiAssistant {
        return ForensicAiAssistant()
    }

    @Provides
    @Singleton
    fun provideMassExtractor(
        @ApplicationContext context: Context,
        lifecycleManager: UsbLifecycleManager
    ): MassExtractor {
        return MassExtractor(context, lifecycleManager)
    }

    @Provides
    @Singleton
    fun provideUsbLifecycleManager(
        @ApplicationContext context: Context,
        usbManager: UsbManager,
        appScope: CoroutineScope,
        coordinator: SessionCoordinator
    ): UsbLifecycleManager {
        return UsbLifecycleManager(context, usbManager, appScope, coordinator)
    }

    @Provides
    @Singleton
    fun provideAdbManager(lifecycleManager: UsbLifecycleManager): AdbManager {
        return AdbManager(lifecycleManager)
    }

    @Provides
    @Singleton
    fun provideAdbSession(lifecycleManager: UsbLifecycleManager): AdbSession {
        // AdbSession requires a UsbTransport. At app startup no device is connected yet,
        // so we use a deferred-resolve pattern: return a session wrapping the lifecycle
        // manager's transport, gracefully handling the null case.
        // The session's connect() will fail gracefully until a device is actually attached.
        val transport: UsbTransport = lifecycleManager.getTransport() ?: NoOpUsbTransport
        return AdbSession(transport)
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
    fun provideIosSessionCoordinator(
        sessionCoordinator: SessionCoordinator,
        hardwareManager: HardwareManager
    ): IosSessionCoordinator {
        return IosSessionCoordinator(sessionCoordinator, hardwareManager)
    }

    @Provides
    @Singleton
    fun provideUniversalExploitOrchestrator(
        appScope: CoroutineScope,
        fuzzDao: FuzzDao,
        sessionCoordinator: SessionCoordinator,
        extractor: PostExploitExtractor
    ): UniversalExploitOrchestrator {
        return UniversalExploitOrchestrator(appScope, fuzzDao, sessionCoordinator, extractor)
    }

    // ── CVE Intelligence ──

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
    fun provideCveImporter(dao: CveDao): CveImporter {
        return CveImporter(dao)
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
    fun provideRamdiskForensicEngine(): RamdiskForensicEngine {
        return RamdiskForensicEngine()
    }

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

/**
 * No-op UsbTransport used as the initial placeholder for singletons that require AdbSession
 * before any real USB device is connected. AdbSession.connect() will fail gracefully when
 * using this transport — the real transport is set by UsbLifecycleManager on device attach.
 */
private object NoOpUsbTransport : UsbTransport {
    private val stub = UsbDescriptorSnapshot(0, 0, 0, 0, 0, null, null, 0, emptyList())

    override val isOpen: Boolean = false
    override val deviceInfo: UsbDescriptorSnapshot = stub

    override suspend fun open() = Result.failure<Unit>(UnsupportedOperationException("NoOp"))
    override suspend fun send(data: ByteArray, timeoutMs: Int) = Result.failure<Int>(UnsupportedOperationException("NoOp"))
    override suspend fun receive(length: Int, timeoutMs: Int)  = Result.failure<ByteArray>(UnsupportedOperationException("NoOp"))
    override suspend fun sendAndReceive(
        data: ByteArray, receiveLength: Int, sendTimeout: Int, receiveTimeout: Int
    ) = Result.failure<ByteArray>(UnsupportedOperationException("NoOp"))
    override suspend fun controlTransfer(
        requestType: Int, request: Int, value: Int, index: Int,
        buffer: ByteArray?, length: Int, timeout: Int
    ) = Result.failure<Int>(UnsupportedOperationException("NoOp"))
    override fun close() = Unit
    override suspend fun write(data: ByteArray, timeoutMs: Int?) =
        TransferResult.NullConnection("NoOp transport — no device connected")
    override suspend fun read(expectedSize: Int, timeoutMs: Int?) =
        TransferResult.NullConnection("NoOp transport — no device connected")
    override suspend fun control(
        requestType: Int, request: Int, value: Int, index: Int,
        data: ByteArray?, timeoutMs: Int?
    ) = TransferResult.NullConnection("NoOp transport — no device connected")
}
