import { Database, Search, Download, Trash2, Cpu, HardDrive, RefreshCw } from "lucide-react";
import { useState, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface Partition {
    name: String;
    size: number;
    offset: number;
    type?: string;
}

export default function PartitionManagerPage() {
    const [searchQuery, setSearchQuery] = useState("");
    const [selectedPartition, setSelectedPartition] = useState<Partition | null>(null);

    // Mock PIT data for Samsung integration demonstration
    const mockPartitions: Partition[] = [
        { name: "SBL1", size: 524288, offset: 0x00000000, type: "Bootloader" },
        { name: "SBL2", size: 524288, offset: 0x00080000, type: "Bootloader" },
        { name: "PARAM", size: 10485760, offset: 0x00100000, type: "Data" },
        { name: "BOOT", size: 67108864, offset: 0x00B00000, type: "Kernel" },
        { name: "RECOVERY", size: 67108864, offset: 0x04B00000, type: "Kernel" },
        { name: "SYSTEM", size: 4294967296, offset: 0x08B00000, type: "OS" },
        { name: "USERDATA", size: 128849018880, offset: 0x108B00000, type: "Storage" },
        { name: "MODEM", size: 134217728, offset: 0x1E0000000, type: "Baseband" },
        { name: "CP_DEBUG", size: 10485760, offset: 0x1E8000000, type: "Debug" },
        { name: "EFS", size: 20971520, offset: 0x1E9000000, type: "NV_Data" },
    ];

    const filteredPartitions = useMemo(() => {
        return mockPartitions.filter(p =>
            p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            p.type?.toLowerCase().includes(searchQuery.toLowerCase())
        );
    }, [searchQuery]);

    const formatSize = (bytes: number) => {
        if (bytes === 0) return "0 B";
        const k = 1024;
        const sizes = ["B", "KB", "MB", "GB", "TB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
    };

    return (
        <div className="flex flex-col h-full w-full bg-[#0D0D1A] text-white p-8 relative overflow-hidden">
            {/* Background Effects */}
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-cyan-500/10 blur-[120px] rounded-full -translate-y-1/2 translate-x-1/2"></div>

            <div className="relative z-10 h-full flex flex-col gap-6">
                {/* Header Section */}
                <div className="flex items-end justify-between">
                    <div className="flex flex-col gap-2">
                        <h1 className="text-3xl font-black flex items-center gap-3">
                            <Database className="text-cyan-400" size={32} />
                            Partition Manager
                        </h1>
                        <p className="text-gray-400 text-sm">
                            Samsung Loke/PIT Engine • Device: <span className="text-cyan-300 font-mono">SM-S928B (Qualcomm 8G3)</span>
                        </p>
                    </div>

                    <div className="flex items-center gap-3">
                        <div className="relative group">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" size={16} />
                            <input
                                type="text"
                                placeholder="Filter partitions..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="bg-white/5 border border-white/10 rounded-xl py-2 pl-10 pr-4 text-sm focus:outline-none focus:border-cyan-400/50 w-64 transition-all"
                            />
                        </div>
                        <button className="p-2.5 bg-white/5 border border-white/10 rounded-xl hover:bg-white/10 transition-colors">
                            <RefreshCw size={18} />
                        </button>
                    </div>
                </div>

                <div className="flex-1 grid grid-cols-1 lg:grid-cols-4 gap-6 min-h-0">
                    {/* Partition Table Area */}
                    <div className="lg:col-span-3 flex flex-col gap-4 min-h-0 bg-white/5 border border-white/10 rounded-2xl overflow-hidden backdrop-blur-md">
                        <div className="overflow-y-auto overflow-x-hidden h-full scrollbar-thin scrollbar-thumb-white/10">
                            <table className="w-full text-left">
                                <thead className="sticky top-0 bg-[#16162A] z-20 border-b border-white/10">
                                    <tr>
                                        <th className="px-6 py-4 text-[10px] font-bold text-gray-500 uppercase tracking-widest">Name</th>
                                        <th className="px-6 py-4 text-[10px] font-bold text-gray-500 uppercase tracking-widest">Offset</th>
                                        <th className="px-6 py-4 text-[10px] font-bold text-gray-500 uppercase tracking-widest">Size</th>
                                        <th className="px-6 py-4 text-[10px] font-bold text-gray-500 uppercase tracking-widest">Type</th>
                                        <th className="px-6 py-4 text-[10px] font-bold text-gray-500 uppercase tracking-widest text-right">Action</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-white/5">
                                    {filteredPartitions.map((p, i) => (
                                        <motion.tr
                                            key={i}
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            transition={{ delay: i * 0.02 }}
                                            onClick={() => setSelectedPartition(p)}
                                            className={`group cursor-pointer hover:bg-white/5 transition-colors ${selectedPartition?.name === p.name ? 'bg-cyan-500/10' : ''}`}
                                        >
                                            <td className="px-6 py-4 font-bold font-mono text-cyan-50 group-hover:text-cyan-400">
                                                {p.name}
                                            </td>
                                            <td className="px-6 py-4 text-xs text-gray-500 font-mono">
                                                0x{p.offset.toString(16).toUpperCase().padStart(8, '0')}
                                            </td>
                                            <td className="px-6 py-4 text-xs text-gray-300">
                                                {formatSize(p.size)}
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className="text-[10px] font-bold uppercase bg-white/5 px-2 py-0.5 rounded text-gray-400">
                                                    {p.type}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <button className="p-1.5 text-gray-500 hover:text-cyan-400 hover:bg-cyan-400/10 rounded-lg transition-all">
                                                    <Download size={14} />
                                                </button>
                                            </td>
                                        </motion.tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Action Inspector Area */}
                    <div className="lg:col-span-1 flex flex-col gap-6">
                        <AnimatePresence mode="wait">
                            {selectedPartition ? (
                                <motion.div
                                    key={selectedPartition.name.toString()}
                                    initial={{ opacity: 0, scale: 0.95 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    exit={{ opacity: 0, scale: 0.95 }}
                                    className="bg-white/5 border border-white/10 rounded-2xl p-6 backdrop-blur-xl flex flex-col gap-6"
                                >
                                    <div className="flex items-center gap-4">
                                        <div className="p-3 bg-cyan-400/10 text-cyan-400 rounded-xl">
                                            <Cpu size={24} />
                                        </div>
                                        <div>
                                            <h3 className="text-xl font-bold truncate max-w-[150px]">{selectedPartition.name}</h3>
                                            <span className="text-xs text-gray-500">{selectedPartition.type} Layer</span>
                                        </div>
                                    </div>

                                    <div className="space-y-4">
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="text-gray-500">Address Range</span>
                                            <span className="font-mono text-xs">0x{selectedPartition.offset.toString(16).toUpperCase()}</span>
                                        </div>
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="text-gray-500">Binary Size</span>
                                            <span className="text-cyan-400 font-bold">{formatSize(selectedPartition.size)}</span>
                                        </div>
                                        <div className="h-px bg-white/5"></div>
                                    </div>

                                    <div className="grid grid-cols-1 gap-3">
                                        <button className="flex items-center justify-center gap-2 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-sm font-bold transition-all">
                                            <Download size={16} className="text-cyan-400" />
                                            Read Memory
                                        </button>
                                        <button className="flex items-center justify-center gap-2 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-sm font-bold transition-all">
                                            <Trash2 size={16} className="text-red-400" />
                                            Wipe Partition
                                        </button>
                                        <button className="flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-cyan-600 to-purple-600 hover:from-cyan-500 hover:to-purple-500 rounded-xl text-sm font-black shadow-lg shadow-cyan-500/10 transition-all">
                                            Flash Partition
                                        </button>
                                    </div>
                                </motion.div>
                            ) : (
                                <div className="h-48 bg-white/5 border border-white/10 border-dashed rounded-2xl flex flex-col items-center justify-center gap-3 text-gray-500 p-6 text-center">
                                    <HardDrive size={32} className="opacity-20" />
                                    <p className="text-xs">Select a partition to view low-level memory actions.</p>
                                </div>
                            )}
                        </AnimatePresence>

                        {/* Hardware Status Peek */}
                        <div className="bg-gradient-to-br from-[#1A1A2E] to-[#0D0D1A] border border-white/5 rounded-2xl p-5 flex flex-col gap-3">
                            <div className="flex justify-between items-center">
                                <span className="text-[10px] font-bold text-gray-500 uppercase tracking-widest">USB Controller</span>
                                <div className="w-1.5 h-1.5 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]"></div>
                            </div>
                            <div className="flex flex-col">
                                <span className="text-xs font-bold">Standard OHCI (USB 2.0)</span>
                                <span className="text-[10px] text-gray-500">Latency: 0.12ms • Status: Stable</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
