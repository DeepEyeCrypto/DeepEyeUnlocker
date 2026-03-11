package com.deepeye.otg.protocol.apple.iphone15

object IphoneCveDatabase {

    private data class CveRule(
        val entry: CveEntry,
        val affected: (String) -> Boolean
    )

    private val cveRules = listOf(
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-23529",
                component = "WebKit",
                type = VulnType.TYPE_CONFUSION,
                affectedIos = "< 16.3.1",
                patched = "16.3.1",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.WEB_CONTENT,
                chainPosition = ChainPosition.INITIAL_VECTOR,
                notes = "Web content triggered type confusion in WebKit renderer process"
            ),
            affected = { v -> isLessThan(v, "16.3.1") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-28205",
                component = "WebKit",
                type = VulnType.USE_AFTER_FREE,
                affectedIos = "< 16.4.1",
                patched = "16.4.1",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.WEB_CONTENT,
                chainPosition = ChainPosition.INITIAL_VECTOR,
                notes = "Often discussed with CVE-2023-28206 kernel escalation"
            ),
            affected = { v -> isLessThan(v, "16.4.1") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-41064",
                component = "ImageIO",
                type = VulnType.BUFFER_OVERFLOW,
                affectedIos = "< 16.6.1",
                patched = "16.6.1",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.ZERO_CLICK,
                chainPosition = ChainPosition.INITIAL_VECTOR,
                notes = "Part of BLASTPASS delivery analysis"
            ),
            affected = { v -> isLessThan(v, "16.6.1") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-32434",
                component = "Kernel",
                type = VulnType.INTEGER_OVERFLOW,
                affectedIos = "< 16.5.1",
                patched = "16.5.1",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.LOCAL_APP,
                chainPosition = ChainPosition.KERNEL,
                pacRequired = true,
                pplRequired = true,
                notes = "Kernel read/write primitive reported in public research"
            ),
            affected = { v -> isLessThan(v, "16.5.1") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-38606",
                component = "Kernel/MMIO",
                type = VulnType.MEMORY_CORRUPTION,
                affectedIos = "< 16.6",
                patched = "16.6",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.LOCAL_APP,
                chainPosition = ChainPosition.PPL,
                pplRequired = true,
                notes = "Publicly documented as a high-complexity PPL bypass class"
            ),
            affected = { v -> isLessThan(v, "16.6") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-41992",
                component = "Kernel",
                type = VulnType.MEMORY_CORRUPTION,
                affectedIos = "< 16.7 / 17.0",
                patched = "16.7 / 17.0",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.LOCAL_APP,
                chainPosition = ChainPosition.KERNEL,
                notes = "LPE used in targeted attacks (public reporting)"
            ),
            affected = { v ->
                (v.startsWith("16.") && isLessThan(v, "16.7")) ||
                    (v.startsWith("17.") && isLessThan(v, "17.0"))
            }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2024-23222",
                component = "WebKit",
                type = VulnType.TYPE_CONFUSION,
                affectedIos = "< 16.7.5 / 17.3",
                patched = "16.7.5 / 17.3",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.WEB_CONTENT,
                chainPosition = ChainPosition.INITIAL_VECTOR,
                notes = "Apple advisory indicated potential in-the-wild exploitation"
            ),
            affected = { v ->
                (v.startsWith("16.") && isLessThan(v, "16.7.5")) ||
                    (v.startsWith("17.") && isLessThan(v, "17.3"))
            }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2024-23296",
                component = "RTKit",
                type = VulnType.MEMORY_CORRUPTION,
                affectedIos = "< 17.4",
                patched = "17.4",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.LOCAL_APP,
                chainPosition = ChainPosition.KERNEL,
                notes = "Highlighted RTKit/coprocessor attack surface"
            ),
            affected = { v -> isLessThan(v, "17.4") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2024-27826",
                component = "Kernel",
                type = VulnType.MEMORY_CORRUPTION,
                affectedIos = "< 17.5",
                patched = "17.5",
                exploitPublic = true,
                deliveryMethod = DeliveryMethod.LOCAL_APP,
                chainPosition = ChainPosition.KERNEL,
                notes = "Kernel memory corruption patched in 17.5"
            ),
            affected = { v -> isLessThan(v, "17.5") }
        ),
        CveRule(
            entry = CveEntry(
                cve = "CVE-2023-42947",
                component = "USB driver",
                type = VulnType.OTHER,
                affectedIos = "< 17.2",
                patched = "17.2",
                exploitPublic = false,
                deliveryMethod = DeliveryMethod.USB_PHYSICAL,
                chainPosition = ChainPosition.USB,
                notes = "Unexpected termination via USB path (public advisory)"
            ),
            affected = { v -> isLessThan(v, "17.2") }
        )
    )

    fun getCvesForDevice(chip: IphoneChip, iosVersion: String): List<CveEntry> {
        if (chip == IphoneChip.UNKNOWN) return emptyList()
        val normalized = normalizeVersion(iosVersion) ?: return cveRules.map { it.entry }
        return cveRules.filter { it.affected(normalized) }.map { it.entry }
    }

    fun getExploitChains(chip: IphoneChip, iosVersion: String): List<ExploitChain> {
        if (chip == IphoneChip.UNKNOWN) return emptyList()

        val normalized = normalizeVersion(iosVersion)
        val triStatus = when {
            normalized == null -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
            inRange(normalized, "16.5.1", "16.6.1") -> ResearchChainStatus.APPLICABLE_PUBLIC_RESEARCH
            isLessThan(normalized, "16.5.1") -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
            else -> ResearchChainStatus.PATCHED_ON_DEVICE
        }

        val rtkitStatus = when {
            normalized == null -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
            isLessThan(normalized, "17.4") -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
            else -> ResearchChainStatus.PATCHED_ON_DEVICE
        }

        val modernStatus = when {
            normalized == null -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
            isGreaterOrEqual(normalized, "17.5") -> ResearchChainStatus.NO_PUBLIC_CHAIN
            else -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
        }

        return listOf(
            ExploitChain(
                chainId = "triangulation_class_16x",
                name = "Operation Triangulation class (16.5.1–16.6)",
                applicableIos = "16.5.1–16.6",
                status = triStatus,
                reliability = if (triStatus == ResearchChainStatus.APPLICABLE_PUBLIC_RESEARCH) 72 else 20,
                notes = "Historical/public research chain family; not a current universal path"
            ),
            ExploitChain(
                chainId = "webkit_kernel_legacy_16x",
                name = "Legacy WebKit → Kernel (16.x windows)",
                applicableIos = "16.0–16.5.x",
                status = when {
                    normalized == null -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
                    isLessThan(normalized, "16.6") -> ResearchChainStatus.LIMITED_PUBLIC_RESEARCH
                    else -> ResearchChainStatus.PATCHED_ON_DEVICE
                },
                reliability = 38,
                notes = "Useful for historical triage and version matching only"
            ),
            ExploitChain(
                chainId = "rtkit_2024",
                name = "RTKit-assisted escalation research",
                applicableIos = "< 17.4",
                status = rtkitStatus,
                reliability = 35,
                notes = "Coprocessor surface remains important research direction"
            ),
            ExploitChain(
                chainId = "ios17_modern",
                name = "iOS 17.5+ modern chain availability",
                applicableIos = "17.5+",
                status = modernStatus,
                reliability = 8,
                notes = "No public full chain for A16/A17 in this range"
            )
        )
    }

    private fun normalizeVersion(version: String?): String? {
        if (version.isNullOrBlank()) return null
        return Regex("(\\d+\\.\\d+(?:\\.\\d+)?)").find(version)?.value
    }

    private fun inRange(version: String, minInclusive: String, maxExclusive: String): Boolean {
        return compareVersions(version, minInclusive) >= 0 && compareVersions(version, maxExclusive) < 0
    }

    private fun isLessThan(version: String, boundary: String): Boolean {
        return compareVersions(version, boundary) < 0
    }

    private fun isGreaterOrEqual(version: String, boundary: String): Boolean {
        return compareVersions(version, boundary) >= 0
    }

    private fun compareVersions(left: String, right: String): Int {
        val l = left.split('.').map { it.toIntOrNull() ?: 0 }
        val r = right.split('.').map { it.toIntOrNull() ?: 0 }
        val max = maxOf(l.size, r.size)
        for (i in 0 until max) {
            val lv = l.getOrElse(i) { 0 }
            val rv = r.getOrElse(i) { 0 }
            if (lv != rv) return lv.compareTo(rv)
        }
        return 0
    }
}
