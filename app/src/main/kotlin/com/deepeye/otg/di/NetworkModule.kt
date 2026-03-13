package com.deepeye.otg.di

import com.deepeye.otg.service.CloudSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("api.deepeye.io", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("api.deepeye.security", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(pinner: CertificatePinner): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(pinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideCloudSyncService(client: OkHttpClient): CloudSyncService {
        return CloudSyncService(client)
    }

    @Provides
    @Singleton
    fun provideCloudClient(client: OkHttpClient): com.deepeye.otg.service.CloudClient {
        return com.deepeye.otg.service.CloudClient(client)
    }

    @Provides
    @Singleton
    fun provideLicenseManager(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        cloudClient: com.deepeye.otg.service.CloudClient
    ): com.deepeye.otg.service.LicenseManager {
        return com.deepeye.otg.service.LicenseManager(context, cloudClient)
    }

    @Provides
    @Singleton
    fun provideTunnelManager(
        client: OkHttpClient,
        usbLifecycleManager: com.deepeye.otg.usb.UsbLifecycleManager
    ): com.deepeye.otg.service.TunnelManager {
        return com.deepeye.otg.service.TunnelManager(client, usbLifecycleManager)
    }
}
