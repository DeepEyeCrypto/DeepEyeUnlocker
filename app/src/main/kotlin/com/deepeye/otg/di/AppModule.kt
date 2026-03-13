package com.deepeye.otg.di

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import com.deepeye.otg.DeepEyeApplication
import com.deepeye.otg.data.SettingsManager
import com.deepeye.otg.engine.ForensicAiAssistant
import com.deepeye.otg.engine.ForensicEngine
import com.deepeye.otg.service.MassExtractor
import com.deepeye.otg.usb.AdbManager
import com.deepeye.otg.usb.HardwareManager
import com.deepeye.otg.usb.SessionCoordinator
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.fuzz.hid.HidFuzzCoordinator
import com.deepeye.otg.fuzz.hid.MaliciousHidDevice
import com.deepeye.otg.exploit.ExploitChainOrchestrator
import com.deepeye.otg.exploit.PostExploitExtractor
import com.deepeye.otg.exploit.AslrDefeater
import com.deepeye.otg.exploit.AmfiSymlinkBypass
import com.deepeye.otg.data.db.dao.FuzzDao
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
    fun provideAppScope(application: Application): CoroutineScope {
        return (application as com.deepeye.otg.DeepEyeApplication).appScope
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
    fun provideHardwareManager(
        @ApplicationContext context: Context,
        lifecycleManager: UsbLifecycleManager
    ): HardwareManager {
        return HardwareManager(context, lifecycleManager)
    }
}
