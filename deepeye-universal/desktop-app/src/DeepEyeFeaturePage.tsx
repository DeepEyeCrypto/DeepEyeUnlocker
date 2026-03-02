import { useState, useEffect } from "react";
import {
    Zap, Download, ShieldCheck, HardDrive,
    Trash2, Store, PackageCheck, Settings2,
    Lock, UserCheck, Building2, Cpu,
    Fingerprint, ScanLine, Unlock, Landmark,
    BadgeCheck, Antenna, Radio, SimCard,
    Info, Terminal, Rocket, AppWindow
} from "lucide-react";
import { motion } from "framer-motion";

// --- TITLE VALIDATION CHECKLIST ---
// 1. Write Firmware
// 2. Read / Backup Firmware
// 3. Backup / Restore Security
// 4. Partition Manager
// 5. Factory Reset / Format
// 6. Demo Mode to Retail
// 7. Safe Wipe with Backup
// 8. Brand Config Presets
// 9. Google FRP Assistant
// 10. Samsung / Mi Account Guide
// 11. Enterprise EFRP Hooks
// 12. MetaMode FRP Flows
// 13. Screen Lock Repair
// 14. Lock State Analysis
// 15. Bootloader Unlock (Official)
// 16. MDM / Finance Lock Handling
// 17. IMEI Integrity Check
// 18. IMEI Restore (Original Only)
// 19. 5G Modem Repair
// 20. Network / SIM Unlock
// 21. Deep Device Info
// 22. Diag / ADB Enabler
// 23. One-Click Root
// 24. ADB App Manager
// =================================== //

interface Feature {
    id: number;
    title: string;
    desc: string;
    tags: string[];
    risk: "safe" | "policy" | "restricted";
    iconName: string;
}

interface FeatureGroupData {
    id: string;
    title: string;
    iconName: string;
    color: string;
    bgAccent: string;
    features: Feature[];
}

const FEATURE_GROUPS: FeatureGroupData[] = [
    {
        id: "flashing",
        title: "Flashing & Firmware",
        iconName: "Zap",
        color: "#6C3EF4",
        bgAccent: "from-purple-900/40 to-purple-800/10",
        features: [
            { id: 1, title: "Write Firmware", desc: "Flash stock or custom ROMs via MTK, Qualcomm, UniSoc, Samsung formats.", tags: ["MTK", "Qualcomm", "UniSoc", "Samsung"], risk: "safe", iconName: "Upload" },
            { id: 2, title: "Read / Backup Firmware", desc: "Full ROM backup before any risky operation. Supports all partition layouts.", tags: ["MTK", "Qualcomm", "UniSoc"], risk: "safe", iconName: "Download" },
            { id: 3, title: "Backup / Restore Security", desc: "Save and restore EFS, NV data, modem certs and device identity.", tags: ["Qualcomm", "Samsung"], risk: "safe", iconName: "ShieldCheck" },
            { id: 4, title: "Partition Manager", desc: "List, read, write or erase individual partitions with fine control.", tags: ["MTK", "Qualcomm"], risk: "policy", iconName: "HardDrive" }
        ]
    },
    {
        id: "reset",
        title: "Reset & Cleanup",
        iconName: "RefreshCw",
        color: "#00C4B4",
        bgAccent: "from-teal-900/40 to-teal-800/10",
        features: [
            { id: 5, title: "Factory Reset / Format", desc: "One-click wipe for resale or reuse across all chipsets and modes.", tags: ["MTK", "Qualcomm", "UniSoc", "Samsung"], risk: "safe", iconName: "Trash2" },
            { id: 6, title: "Demo Mode to Retail", desc: "Convert retail demo units to normal consumer mode (proof required).", tags: ["Samsung", "Qualcomm"], risk: "policy", iconName: "Store" },
            { id: 7, title: "Safe Wipe with Backup", desc: "Guided wipe flow that always prompts backup first to prevent data loss.", tags: ["All Chipsets"], risk: "safe", iconName: "PackageCheck" },
            { id: 8, title: "Brand Config Presets", desc: "Pre-tuned reset profiles for Samsung, Xiaomi, Infinix, Tecno and more.", tags: ["Samsung", "MTK"], risk: "safe", iconName: "Settings2" }
        ]
    },
    {
        id: "frp",
        title: "FRP & Account Assistance",
        iconName: "KeyRound",
        color: "#F59E0B",
        bgAccent: "from-amber-900/40 to-amber-800/10",
        features: [
            { id: 9, title: "Google FRP Assistant", desc: "Detect FRP status and guide users to official Google account recovery.", tags: ["Samsung", "Qualcomm", "MTK"], risk: "policy", iconName: "Lock" },
            { id: 10, title: "Samsung / Mi Account Guide", desc: "Explain OEM account locks and open official unlock portals for users.", tags: ["Samsung"], risk: "policy", iconName: "UserCheck" },
            { id: 11, title: "Enterprise EFRP Hooks", desc: "Show when a company admin can clear FRP via MDM or EFRP configuration.", tags: ["Enterprise"], risk: "policy", iconName: "Building2" },
            { id: 12, title: "MetaMode FRP Flows", desc: "Chipset-aware wizards for MetaMode FRP handling on MTK devices.", tags: ["MTK"], risk: "policy", iconName: "Cpu" }
        ]
    },
    {
        id: "locks",
        title: "Locks & Security",
        iconName: "ShieldAlert",
        color: "#EF4444",
        bgAccent: "from-red-900/40 to-red-800/10",
        features: [
            { id: 13, title: "Screen Lock Repair", desc: "Help owners recover devices after forgotten PIN, pattern, or password.", tags: ["MTK", "Qualcomm", "UniSoc"], risk: "policy", iconName: "Fingerprint" },
            { id: 14, title: "Lock State Analysis", desc: "Analyze lock type to pick the safest official recovery route automatically.", tags: ["All Chipsets"], risk: "safe", iconName: "ScanLine" },
            { id: 15, title: "Bootloader Unlock (Official)", desc: "OEM-approved guided unlock for custom ROMs and rooting workflows.", tags: ["Qualcomm", "MTK"], risk: "policy", iconName: "Unlock" },
            { id: 16, title: "MDM / Finance Lock Handling", desc: "Legal, policy-driven flows for enterprise MDM and financing locks.", tags: ["Enterprise", "Samsung"], risk: "restricted", iconName: "Landmark" }
        ]
    },
    {
        id: "imei",
        title: "IMEI & Network",
        iconName: "Signal",
        color: "#F97316",
        bgAccent: "from-orange-900/40 to-orange-800/10",
        features: [
            { id: 17, title: "IMEI Integrity Check", desc: "Compare live IMEI against original data to detect corruption or mismatch.", tags: ["MTK", "Qualcomm", "UniSoc"], risk: "safe", iconName: "BadgeCheck" },
            { id: 18, title: "IMEI Restore (Original Only)", desc: "Repair null or corrupted IMEI back to factory value with proof verification.", tags: ["MTK", "Qualcomm"], risk: "restricted", iconName: "Antenna" },
            { id: 19, title: "5G Modem Repair", desc: "Advanced repair tools for modern 5G radio stacks (CPID supported).", tags: ["Qualcomm", "MTK"], risk: "restricted", iconName: "Radio" },
            { id: 20, title: "Network / SIM Unlock", desc: "Carrier-compliant SIM unlock guidance and tools, fully region-aware.", tags: ["Qualcomm", "Samsung"], risk: "restricted", iconName: "SimCard" }
        ]
    },
    {
        id: "advanced",
        title: "Advanced & Diagnostics",
        iconName: "Terminal",
        color: "#22C55E",
        bgAccent: "from-green-900/40 to-green-800/10",
        features: [
            { id: 21, title: "Deep Device Info", desc: "One-click snapshot: model, SoC, security level, bootloader & FRP state.", tags: ["All Chipsets"], risk: "safe", iconName: "Info" },
            { id: 22, title: "Diag / ADB Enabler", desc: "Safely open diagnostic and ADB channels for authorized service work.", tags: ["Samsung", "Qualcomm", "MTK"], risk: "policy", iconName: "Terminal" },
            { id: 23, title: "One-Click Root", desc: "Magisk-based root for 450+ Samsung and 170+ Xiaomi supported builds.", tags: ["Samsung", "MTK"], risk: "policy", iconName: "Rocket" },
            { id: 24, title: "ADB App Manager", desc: "List, disable, uninstall or install APKs directly via ADB from desktop.", tags: ["All Chipsets"], risk: "safe", iconName: "AppWindow" }
        ]
    }
];

const IconMap: Record<string, any> = {
    Zap, Download, ShieldCheck, HardDrive,
    Trash2, Store, PackageCheck, Settings2,
    Lock, UserCheck, Building2, Cpu,
    Fingerprint, ScanLine, Unlock, Landmark,
    BadgeCheck, Antenna, Radio, SimCard,
    Info, Terminal, Rocket, AppWindow
};

// -------------------------------------------------------------
// COMPONENTS
// -------------------------------------------------------------

function TagPill({ tag }: { tag: string }) {
    return (
        <span className="text-[9px] px-1.5 py-0.5 rounded bg-white/10 text-white/50 font-medium">
            {tag}
        </span>
    );
}

function RiskBadge({ risk }: { risk: string }) {
    if (risk === "safe") {
        return (
            <div className="text-[10px] px-2 py-0.5 rounded-full bg-green-500/20 text-green-400 font-semibold ml-auto flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-current"></span> SAFE
            </div>
        );
    }
    if (risk === "policy") {
        return (
            <div className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-400 font-semibold ml-auto flex items-center gap-1">
                <span className="text-[10px]">⚠</span> POLICY
            </div>
        );
    }
    return (
        <div className="text-[10px] px-2 py-0.5 rounded-full bg-red-500/20 text-red-400 font-semibold ml-auto flex items-center gap-1">
            <span className="text-[10px]">🔒</span> AUTH
        </div>
    );
}

function FeatureCard({ feature, color }: { feature: Feature, color: string }) {
    const IconCmp = IconMap[feature.iconName] || Zap;
    return (
        <motion.div
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="group relative flex flex-col gap-2 p-4 rounded-xl bg-[#1A1A2E] border border-white/5 hover:border-purple-500/50 hover:bg-[#252542] transition-all duration-200 cursor-pointer"
        >
            <div className="flex items-center w-full">
                <IconCmp size={20} color={color} />
                <RiskBadge risk={feature.risk} />
            </div>
            <h3 className="text-sm font-bold text-white leading-tight mt-1">{feature.title}</h3>
            <p className="text-xs text-[#9898C4] leading-relaxed mt-1">{feature.desc}</p>
            <div className="flex flex-wrap gap-1 mt-auto pt-2">
                {feature.tags.map(t => <TagPill key={t} tag={t} />)}
            </div>
        </motion.div>
    );
}

function FeatureGroup({ group, index }: { group: FeatureGroupData, index: number }) {
    const IconCmp = IconMap[group.iconName] || Zap;

    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className={`rounded-2xl border border-white/10 overflow-hidden bg-gradient-to-br ${group.bgAccent} flex flex-col`}
        >
            <div className="flex items-center gap-3 px-6 py-4 border-b border-white/10 shrink-0">
                <div className="p-2 rounded-full flex items-center justify-center" style={{ background: `${group.color}20` }}>
                    <IconCmp size={18} color={group.color} />
                </div>
                <h3 className="text-lg font-bold text-white">{group.title}</h3>
                <span className="ml-auto text-xs px-2 py-1 rounded-full bg-white/10 text-white/60 font-medium">
                    4 functions
                </span>
            </div>

            <div className="grid grid-cols-2 gap-3 p-4 h-full">
                {group.features.map(f => (
                    <FeatureCard key={f.id} feature={f} color={group.color} />
                ))}
            </div>
        </motion.div>
    );
}

function AnimatedStat({ value, label }: { value: string, label: string }) {
    return (
        <div className="flex flex-col items-center gap-3 p-6 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm min-w-[160px] w-full sm:w-auto">
            <div className="text-6xl font-black text-white tracking-tight">{value}</div>
            <div className="text-xs font-bold tracking-[0.2em] text-purple-300 uppercase">{label}</div>
        </div>
    );
}

function HeroStatsStrip() {
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);

    return (
        <div className="w-full bg-gradient-to-b from-[#1A0A3A] to-[#0D0D1A] py-16 px-6">
            <div className="flex flex-col sm:flex-row justify-center items-center gap-8 max-w-2xl mx-auto">
                <AnimatedStat value={mounted ? "257" : "0"} label="Brands" />
                <AnimatedStat value={mounted ? "7K+" : "0"} label="Models" />
                <AnimatedStat value={mounted ? "24" : "0"} label="Functions" />
            </div>
        </div>
    );
}

function CoverageStrip() {
    const PREVIEW_BRANDS = ["Samsung", "Xiaomi", "Infinix", "Tecno", "Realme", "Motorola", "OPPO", "Vivo", "OnePlus", "LG", "Huawei", "Nokia"];
    return (
        <div className="py-12 px-6 border-t border-white/5 max-w-7xl mx-auto flex flex-col lg:flex-row gap-8 items-center w-full">
            <div className="w-full lg:w-1/2 flex flex-col gap-4">
                <h3 className="text-2xl font-bold text-white">Coverage at a Glance</h3>
                <p className="text-[#9898C4] text-sm">257 brands &middot; 7071 models spanning flagships to regional devices</p>
                <div className="flex flex-wrap gap-2 mt-2">
                    {["Android 13 · 14 · 15", "One UI 5/6/7", "MIUI 14 / HyperOS"].map(p => (
                        <span key={p} className="bg-white/5 border border-white/10 rounded-full px-3 py-1 text-xs text-gray-300">{p}</span>
                    ))}
                </div>
            </div>

            <div className="w-full lg:w-1/2 flex flex-col gap-4">
                <div className="flex flex-wrap gap-2">
                    {PREVIEW_BRANDS.map(b => (
                        <span key={b} className="bg-white/5 border border-transparent hover:border-white/10 rounded-full px-3 py-1.5 text-sm whitespace-nowrap text-gray-200 transition-colors cursor-default">
                            {b}
                        </span>
                    ))}
                </div>
                <button className="text-purple-400 text-sm font-semibold hover:text-purple-300 transition-colors self-start mt-2">
                    + 245 more brands &rarr;
                </button>
            </div>
        </div>
    );
}

function TechHighlightsBand() {
    const protocols = [
        { label: "MTK V5 / V6 Protocol", color: "#6C3EF4" },
        { label: "Qualcomm EDL + Firehose", color: "#00E5FF" },
        { label: "UniSoc PAC + RPMB", color: "#F59E0B" },
        { label: "Download, ADB, Fastboot, Meta, Diag", color: "#22C55E" },
        { label: "SLA Auth v1/v3/v5", color: "#EF4444" }
    ];

    return (
        <div className="bg-white/5 border-y border-white/5 py-4 px-6 w-full">
            <div className="max-w-7xl mx-auto flex flex-wrap gap-3 items-center justify-center">
                {protocols.map(p => (
                    <div key={p.label} className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 text-xs font-medium text-gray-200">
                        <span className="w-2 h-2 rounded-full" style={{ backgroundColor: p.color }}></span>
                        {p.label}
                    </div>
                ))}
            </div>
        </div>
    );
}

function FeatureGroupsSection() {
    return (
        <div className="w-full py-12">
            <h2 className="text-3xl md:text-4xl font-bold text-white text-center mb-3">
                What DeepEye Universal Can Do
            </h2>
            <p className="text-[#9898C4] text-center mb-12 max-w-xl mx-auto text-sm md:text-base px-4">
                24 pro-grade service functions across all major Android chipsets
            </p>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 px-6 max-w-7xl mx-auto pb-16">
                {FEATURE_GROUPS.map((g, i) => (
                    <FeatureGroup key={g.id} group={g} index={i} />
                ))}
            </div>
        </div>
    );
}

export default function DeepEyeFeaturePage() {
    return (
        <div className="min-h-screen bg-[#0D0D1A] text-white overflow-x-hidden font-sans selection:bg-purple-500/30">
            <HeroStatsStrip />
            <TechHighlightsBand />
            <FeatureGroupsSection />
            <CoverageStrip />

            <footer className="py-8 px-6 border-t border-white/5 flex flex-wrap justify-between items-center gap-4 max-w-7xl mx-auto w-full">
                <div className="flex flex-col gap-1">
                    <span className="text-lg font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent">
                        DeepEye Universal
                    </span>
                    <span className="text-xs text-[#9898C4]">
                        Multi-platform service suite for modern Android devices
                    </span>
                </div>
                <div className="flex items-center gap-4 text-sm font-medium">
                    <button className="text-gray-400 hover:text-white transition-colors">Documentation</button>
                    <button className="text-gray-400 hover:text-white transition-colors">Support</button>
                    <button className="text-gray-400 hover:text-white transition-colors">Pricing</button>
                    <span className="text-gray-600 ml-4">&copy; 2026 DeepEye</span>
                </div>
            </footer>
        </div>
    );
}
