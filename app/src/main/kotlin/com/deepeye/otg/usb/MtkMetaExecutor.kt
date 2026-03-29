package com.deepeye.otg.usb

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MtkMetaExecutor @Inject constructor() {

    fun buildWriteImeiCommand(imei: String): String {
        return "AT+EGMR=1,7,\"$imei\""
    }

    fun buildReadImeiCommand(): String {
        return "AT+EGMR=0,7"
    }

    fun validateImei(imei: String): Boolean {
        return imei.length in 14..16 && imei.all { it.isDigit() }
    }
}

