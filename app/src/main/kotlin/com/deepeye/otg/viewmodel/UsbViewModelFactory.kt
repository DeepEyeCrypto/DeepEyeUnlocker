package com.deepeye.otg.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.deepeye.otg.data.SettingsManager
import com.deepeye.otg.usb.UsbLifecycleManager

/**
 * Stable factory for [UsbViewModel] so Activity does not manually construct or own USB state.
 */
class UsbViewModelFactory(
    private val appContext: Context,
    private val lifecycleManager: UsbLifecycleManager,
    private val settings: SettingsManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsbViewModel::class.java)) {
            return UsbViewModel(
                appContext = appContext,
                lifecycleManager = lifecycleManager,
                settings = settings
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
