package com.deepeye.otg.domain.engine.mtk

import java.util.Locale

/**
 * Common utilities for MTK protocols.
 */

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun Int.toHex(): String = "0x" + Integer.toHexString(this).uppercase(Locale.ROOT)
