import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface ActivationRecord {
    imei?: string;
    meid?: string;
    serial?: string;
    unique_device_id?: string;
    device_class?: string;
    activation_state: string;
    ticket_present: boolean;
    ticket_valid: boolean;
    ticket_source: string;
    signed_fields: string[];
}

export const TicketEngine: React.FC = () => {
    const [path, setPath] = useState("");
    const [record, setRecord] = useState<ActivationRecord | null>(null);
    const [, setStatus] = useState("Idle");

    const parseTicket = async () => {
        try {
            setStatus("Parsing...");
            const res = await invoke<ActivationRecord>('ios_parse_activation_record', { backupPath: path });
            setRecord(res);
            setStatus("Ready");
        } catch (e) {
            console.error(e);
            setStatus("Error");
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-emerald-400">ACTIVATION TICKET ENGINE</h2>
                {record && (
                    <div className={`px-3 py-1 rounded-full border text-[9px] font-black tracking-widest ${
                        record.ticket_source === 'Apple' ? 'border-emerald-500 text-emerald-400' : 'border-amber-500 text-amber-500'
                    }`}>
                        SOURCE: {record.ticket_source}
                    </div>
                )}
            </div>

            <div className="flex gap-2 mb-8">
                <input 
                    type="text" 
                    placeholder="Path to wildcard.plist or backup root"
                    value={path}
                    onChange={(e) => setPath(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-emerald-500 transition-all font-mono"
                />
                <button 
                    onClick={parseTicket}
                    className="px-6 py-3 bg-emerald-600/20 text-emerald-400 border border-emerald-600/40 rounded-xl hover:bg-emerald-600 hover:text-white transition-all font-bold"
                >
                    ANALYZE
                </button>
            </div>

            {record && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 animate-in slide-in-from-right-4 duration-500">
                    <div className="space-y-4">
                        <div className="text-[10px] font-bold text-gray-500 border-b border-white/10 pb-2">PRIMARY RECORD IDENTITY</div>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <div className="text-[8px] text-gray-600 uppercase">State</div>
                                <div className="text-xs font-bold">{record.activation_state}</div>
                            </div>
                            <div>
                                <div className="text-[8px] text-gray-600 uppercase">Class</div>
                                <div className="text-xs font-bold">{record.device_class || 'N/A'}</div>
                            </div>
                            <div>
                                <div className="text-[8px] text-gray-600 uppercase">IMEI</div>
                                <div className="text-xs font-bold text-emerald-400">{record.imei || 'N/A'}</div>
                            </div>
                            <div>
                                <div className="text-[8px] text-gray-600 uppercase">Serial</div>
                                <div className="text-xs font-bold">{record.serial || 'N/A'}</div>
                            </div>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <div className="text-[10px] font-bold text-gray-500 border-b border-white/10 pb-2">SIGNATURE MANIFEST</div>
                        <div className="flex flex-wrap gap-2">
                            {record.signed_fields.map((field, i) => (
                                <span key={i} className="px-2 py-1 bg-white/5 border border-white/5 rounded text-[7px] text-gray-400 hover:text-white transition-colors cursor-default">
                                    {field}
                                </span>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
