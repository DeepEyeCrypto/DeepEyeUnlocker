package com.deepeye.otg.network

import android.net.TrafficStats
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Application Interceptor that explicitly tags threads to prevent StrictMode UntaggedSocketViolation.
 * This runs before OkHttp acquires or creates a connection, ensuring the thread is tagged
 * when the underlying native socket() syscall is lazily triggered.
 */
class TrafficStatsInterceptor(private val tag: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldTag = TrafficStats.getThreadStatsTag()
        try {
            TrafficStats.setThreadStatsTag(tag)
            return chain.proceed(chain.request())
        } finally {
            if (oldTag == -1) {
                TrafficStats.clearThreadStatsTag()
            } else {
                TrafficStats.setThreadStatsTag(oldTag)
            }
        }
    }
}

/**
 * Centralized provider for Network Clients.
 */
object NetworkClient {
    private val baseClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Get an OkHttpClient strictly tagged with the specified TrafficTag.
     */
    fun getClient(tag: Int): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor(TrafficStatsInterceptor(tag))
            .build()
    }
}
