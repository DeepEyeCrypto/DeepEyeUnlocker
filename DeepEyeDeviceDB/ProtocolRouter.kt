package com.deepeye.otg.data.device

import timber.log.Timber

/**
 * ProtocolRouter — auto-routes USB device to correct protocol session
 * Based on DeviceDatabase (1879 devices, 29 brands)
 *
 * Priority order:
 *   1. VID-based fast path (no model needed)
 *   2. Brand+Model exact match from DB
 *   3. Brand-only match from DB
 *   4. UNKNOWN → manual selection
 */
object ProtocolRouter {

    data class RoutingResult(
        val protocol:   DeviceProtocol,
        val confidence: RoutingConfidence,
        val brand:      String?,
        val model:      String?,
        val entry:      DeviceEntry?,
        val reason:     String,
    )

    enum class RoutingConfidence {
        HIGH,    // exact brand+model match from DB
        MEDIUM,  // VID match or brand-only match
        LOW,     // inferred from partial data
        UNKNOWN  // no match found
    }

    /**
     * Primary entry point — call from IosOtgSession / MtkSession
     * after USB device is attached and descriptor is read.
     */
    fun route(
        vid:   Int,
        pid:   Int,
        brand: String? = null,
        model: String? = null,
    ): RoutingResult {
        // Step 1: VID fast path
        val vidProto = DeviceDatabase.protocolFromVid(vid)
        if (vidProto != DeviceProtocol.UNKNOWN && brand == null) {
            Timber.d("[ROUTER] vid=0x${vid.toString(16)} " +
                     "→ protocol=$vidProto confidence=MEDIUM")
            return RoutingResult(
                protocol   = vidProto,
                confidence = RoutingConfidence.MEDIUM,
                brand      = null,
                model      = null,
                entry      = null,
                reason     = "VID 0x${vid.toString(16)} matched",
            )
        }

        // Step 2: Brand + Model exact match
        if (brand != null && model != null) {
            val entry = DeviceDatabase.findByBrandModel(brand, model)
            if (entry != null) {
                Timber.d("[ROUTER] brand=$brand model=$model " +
                         "→ protocol=${entry.protocol} confidence=HIGH")
                return RoutingResult(
                    protocol   = entry.protocol,
                    confidence = RoutingConfidence.HIGH,
                    brand      = brand,
                    model      = model,
                    entry      = entry,
                    reason     = "Exact match: $brand $model",
                )
            }
        }

        // Step 3: Brand-only match
        if (brand != null) {
            val proto = DeviceDatabase.protocolForBrand(brand)
            if (proto != DeviceProtocol.UNKNOWN) {
                Timber.d("[ROUTER] brand=$brand " +
                         "→ protocol=$proto confidence=MEDIUM")
                return RoutingResult(
                    protocol   = proto,
                    confidence = RoutingConfidence.MEDIUM,
                    brand      = brand,
                    model      = model,
                    entry      = null,
                    reason     = "Brand match: $brand",
                )
            }
        }

        // Step 4: VID with brand context
        if (vidProto != DeviceProtocol.UNKNOWN) {
            return RoutingResult(
                protocol   = vidProto,
                confidence = RoutingConfidence.LOW,
                brand      = brand,
                model      = model,
                entry      = null,
                reason     = "VID fallback: 0x${vid.toString(16)}",
            )
        }

        // Unknown
        Timber.w("[ROUTER] no match vid=0x${vid.toString(16)} " +
                 "pid=0x${pid.toString(16)} brand=$brand model=$model")
        return RoutingResult(
            protocol   = DeviceProtocol.UNKNOWN,
            confidence = RoutingConfidence.UNKNOWN,
            brand      = brand,
            model      = model,
            entry      = null,
            reason     = "No match found",
        )
    }

    /**
     * For Xiaomi: determine MTK vs QC from series
     * Redmi/POCO → MTK_V6 | Mi/flagship → QC_EDL
     */
    fun resolveXiaomi(model: String, series: String?): DeviceProtocol {
        val s = series?.lowercase() ?: model.lowercase()
        return when {
            "redmi" in s || "poco" in s -> DeviceProtocol.MTK_V6
            "mi " in s || s.startsWith("mi") -> DeviceProtocol.QC_EDL
            else -> DeviceProtocol.MTK_OR_QC
        }
    }

    /**
     * For OPLUS (Realme/OPPO/Vivo): V6 for 2021+, classic for older
     */
    fun resolveOplusYear(year: Int): DeviceProtocol =
        if (year >= 2021) DeviceProtocol.MTK_V6
        else DeviceProtocol.MTK_BROM
}
