import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface DeviceIdentity {
    udid: string;
    ecid?: string;
    imei?: string;
    serial?: string;
    board_id?: string;
    chip_id?: string;
    is_cdma: boolean;
    imei_valid: boolean;
}

export const IdentityForensics: React.FC = () => {
    const [udid, setUdid] = useState("");
    const [identity, setIdentity] = useState<DeviceIdentity | null>(null);
    const [status, setStatus] = useState("Idle");
    const [error, setError] = useState<string | null>(null);

    const fetchIdentity = async () => {
        if (!udid && !identity) {
            setError("Please enter a UDID or connect a device first.");
            return;
        }
        try {
            setError(null);
            setStatus("Querying...");
            const res = await invoke<DeviceIdentity>('ios_device_identity', { udid });
            setIdentity(res);
            setStatus("Complete");
        } catch (e: any) {
            console.error(e);
            setError(e.toString());
            setStatus("Error");
        }
    };

    const StatusBadge = ({ label, active }: { label: string, active: boolean }) => (
        <div className={`px-2 py-0.5 rounded border text-[8px] font-bold ${active ? 'border-blue-400 text-blue-400' : 'border-red-400/30 text-red-300/40'}`}>
            {label}
        </div>
    );

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-blue-400">DEVICE IDENTITY & TELEPHONY AUDIT</h2>
                {identity && (
                    <div className="flex gap-2">
                        <StatusBadge label="CDMA/MEID" active={identity.is_cdma} />
                        <StatusBadge label="LUHN VALID" active={identity.imei_valid} />
                    </div>
                )}
            </div>

            <div className="flex gap-2 mb-8">
                <input 
                    type="text" 
                    placeholder="Enter Device UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-blue-500 transition-all font-mono"
                />
                <button 
                    onClick={fetchIdentity}
                    className="px-6 py-3 bg-blue-600/20 text-blue-400 border border-blue-600/40 rounded-xl hover:bg-blue-600 hover:text-white transition-all font-bold"
                >
                    EXTRACT IDENTITY
                </button>
            </div>

            {error && (
                <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 animate-in fade-in duration-300">
                    <span className="font-bold underline uppercase mr-2 text-[10px]">Critical Error:</span> {error}
                </div>
            )}

            {identity && (
                <div className="grid grid-cols-2 md:grid-cols-3 gap-6 animate-in zoom-in duration-300">
                    {[
                        { label: "ECID (HEX)", val: identity.ecid, col: "text-blue-400" },
                        { label: "IMEI", val: identity.imei, col: "text-white" },
                        { label: "Serial", val: identity.serial, col: "text-white" },
                        { label: "Chip ID", val: identity.chip_id, col: "text-gray-400" },
                        { label: "Board ID", val: identity.board_id, col: "text-gray-400" },
                        { label: "UDID", val: identity.udid, col: "text-blue-300" },
                    ].map((item, i) => (
                        <div key={i} className="p-4 bg-white/5 border border-white/5 rounded-xl space-y-1">
                            <div className="flex justify-between">
                                <div className="text-[8px] text-gray-500 font-black uppercase tracking-widest">{item.label}</div>
                                {status === "Querying..." && <div className="animate-pulse size-1.5 rounded-full bg-blue-500" />}
                            </div>
                            <div className={`font-bold break-all text-[11px] ${item.col}`}>{item.val || 'N/A'}</div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};
