import React, { useState, useEffect, useRef } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

// --- Interfaces ---
interface SshTunnel {
    local_port: number;
    remote_port: number;
    connected: boolean;
    device_root: boolean;
    mount_state: string;
}

interface MountResult {
    tunnel: SshTunnel;
    mnt1_writable: boolean;
    mnt2_writable: boolean;
    disk_info: string;
    stage_message: string;
}

interface TetheredState {
    bypass_active: boolean;
    reboot_safe: boolean;
    requires_re_exploit: boolean;
    ssh_accessible: boolean;
    current_patches: string[];
    stage_message: string;
}

interface UntetheredState {
    nvram_persistent: boolean;
    system_version_patched: boolean;
    launch_daemon_installed: boolean;
    reboot_safe: boolean;
    stage_message: string;
}

export const FilesystemEngine: React.FC = () => {
    // State
    const [logs, setLogs] = useState<string[]>([]);
    const [loading, setLoading] = useState<string | null>(null);
    
    // Mount & SSH State
    const [tunnelInfo, setTunnelInfo] = useState<SshTunnel | null>(null);
    const [mountInfo, setMountInfo] = useState<MountResult | null>(null);
    const [sshPort, setSshPort] = useState<number>(2222);

    // Persistence State
    const [tetheredState, setTetheredState] = useState<TetheredState | null>(null);

    const logEndRef = useRef<HTMLDivElement>(null);

    // Auto-scroll logs
    useEffect(() => {
        logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [logs]);

    // Setup event listeners for Rust macros
    useEffect(() => {
        const unlistenFs = listen<string>('fs-log', (event) => {
            setLogs((prev) => [...prev, event.payload]);
        });
        const unlistenPersist = listen<string>('persist-log', (event) => {
            setLogs((prev) => [...prev, event.payload]);
        });

        return () => {
            unlistenFs.then((f) => f());
            unlistenPersist.then((f) => f());
        };
    }, []);

    const clearLogs = () => setLogs([]);

    // --- Command Handlers ---
    
    const handleStartTunnel = async () => {
        setLoading('Starting SSH Tunnel...');
        try {
            const res = await invoke<SshTunnel>('fs_start_tunnel', { localPort: sshPort, devicePort: 44 });
            setTunnelInfo(res);
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleMountRW = async () => {
        setLoading('Mounting Read-Write...');
        try {
            const res = await invoke<MountResult>('fs_mount_readwrite', { sshPort });
            setMountInfo(res);
            setTunnelInfo(res.tunnel);
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleSetupAppPatch = async () => {
        setLoading('Patching Setup.app...');
        try {
            await invoke('fs_patch_setup_app', { sshPort });
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleActivationPatch = async () => {
        setLoading('Injecting Activation Record...');
        try {
            await invoke('fs_patch_activation', { sshPort, activationRecordPath: null });
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleLockdownPatch = async () => {
        setLoading('Patching Lockdown/Hosts...');
        try {
            await invoke('fs_patch_lockdown', { sshPort });
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleCheckTethered = async () => {
        setLoading('Checking Persistence...');
        try {
            const res = await invoke<TetheredState>('persist_check_tethered', { sshPort });
            setTetheredState(res);
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleInstallUntethered = async () => {
        setLoading('Installing Untethered Persistence...');
        try {
            await invoke<UntetheredState>('persist_install_untethered', { sshPort });
            // Re-check tethered status
            handleCheckTethered();
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleRemoveUntethered = async () => {
        setLoading('Removing Untethered Persistence...');
        try {
            await invoke('persist_remove_untethered', { sshPort });
            handleCheckTethered();
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleRestoreSetupApp = async () => {
        setLoading('Restoring Setup.app...');
        try {
            await invoke('fs_restore_setup_app', { sshPort });
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleRestoreLockdown = async () => {
        setLoading('Restoring Lockdown/Hosts...');
        try {
            await invoke('fs_restore_lockdown', { sshPort });
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    const handleAutoExploitAndMount = async () => {
        setLoading('Automated DFU Transition...');
        try {
            const sessionId = `auto_exploit_${Date.now()}`;
            setLogs((prev) => [...prev, `[INFO] Starting auto-exploit (session: ${sessionId})`]);
            
            // 1. Detect Device
            setLogs((prev) => [...prev, `[INFO] Detecting device in DFU/Recovery...`]);
            const device = await invoke<any>('hello_bypass_detect', { sessionId });
            if (!device || !device.chip_id) {
                throw new Error("Could not detect vulnerable device in DFU mode.");
            }
            
            // 2. Run Checkm8/Exploit
            setLogs((prev) => [...prev, `[INFO] Running exploit for ${device.chip_name}...`]);
            await invoke('hello_bypass_run', { 
                sessionId, 
                deviceId: device.udid, 
                exploitMethod: device.exploit_method 
            });

            setLogs((prev) => [...prev, `[INFO] Exploit successful. Waiting for device to boot (5s)...`]);
            
            // 3. Wait for SSH server to spin up
            await new Promise((resolve) => setTimeout(resolve, 5000));

            // 4. Start Tunnel
            setLogs((prev) => [...prev, `[INFO] Starting SSH Tunnel on port ${sshPort}...`]);
            const tunnelRes = await invoke<SshTunnel>('fs_start_tunnel', { localPort: sshPort, devicePort: 44 });
            setTunnelInfo(tunnelRes);

            if (!tunnelRes.connected) {
                throw new Error("SSH tunnel failed to connect. Is the device fully booted?");
            }

            // 5. Mount RW
            setLogs((prev) => [...prev, `[INFO] Mounting Filesystem R/W...`]);
            const mountRes = await invoke<MountResult>('fs_mount_readwrite', { sshPort });
            setMountInfo(mountRes);
            setTunnelInfo(mountRes.tunnel);

            setLogs((prev) => [...prev, `[✅] Automated Exploit & Mount Sequence Complete!`]);
        } catch (e: any) {
            setLogs((prev) => [...prev, `[ERROR] ${e}`]);
        } finally {
            setLoading(null);
        }
    };

    return (
        <div className="p-6 h-full flex flex-col gap-6 text-white font-mono text-xs">
            {/* Header */}
            <div className="flex flex-col gap-2 border-b border-white/10 pb-4">
                <h1 className="text-2xl font-bold tracking-tighter text-red-500 uppercase">
                    Filesystem Exploitation Engine
                </h1>
                <p className="text-white/50 text-sm">
                    Advanced SSH-based filesystem bypass orchestration. Device must be in a jailbroken/pwned state with SSH exposed via USB multiplexing.
                </p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 flex-1 min-h-0">
                {/* Left Column: Controls */}
                <div className="flex flex-col gap-6 overflow-y-auto pr-2">
                    
                    {/* SECTION 1: Tunnel & Mount */}
                    <div className="bg-black/40 border border-white/10 rounded-xl p-5 backdrop-blur-xl flex flex-col gap-4">
                        <div className="flex justify-between items-center">
                            <h2 className="text-lg font-bold text-white/80">1. SSH & Mount Control</h2>
                            <div className="flex gap-2 items-center">
                                <span className="text-white/40">Port:</span>
                                <input 
                                    type="number" 
                                    value={sshPort} 
                                    onChange={(e) => setSshPort(Number(e.target.value))}
                                    className="bg-white/5 border border-white/10 p-1 w-20 text-center rounded outline-none focus:border-red-500"
                                />
                            </div>
                        </div>

                        <div className="flex flex-col gap-3">
                            <button 
                                onClick={handleAutoExploitAndMount}
                                disabled={!!loading}
                                className="bg-red-500/20 hover:bg-red-500/40 border border-red-500/50 text-red-100 py-3 rounded-lg transition-colors font-bold disabled:opacity-50"
                            >
                                AUTO-EXPLOIT & MOUNT (DFU)
                            </button>
                            <div className="grid grid-cols-2 gap-3">
                            <button 
                                onClick={handleStartTunnel}
                                disabled={!!loading}
                                className="bg-white/5 hover:bg-white/10 border border-white/10 py-3 rounded-lg transition-colors font-bold disabled:opacity-50"
                            >
                                START IPROXY TUNNEL
                            </button>
                            <button 
                                onClick={handleMountRW}
                                disabled={!!loading || !tunnelInfo?.connected}
                                className="bg-white/5 hover:bg-white/10 border border-white/10 py-3 rounded-lg transition-colors font-bold disabled:opacity-50"
                            >
                                MOUNT R/W (MNT1/MNT2)
                            </button>
                        </div>
                        </div>

                        {/* Status Indicators */}
                        <div className="flex flex-col gap-2 mt-2 bg-white/5 p-3 rounded border border-white/5">
                            <div className="flex justify-between">
                                <span className="text-white/50">SSH Tunnel Status:</span>
                                <span className={tunnelInfo?.connected ? "text-green-400 font-bold" : "text-red-400 font-bold"}>
                                    {tunnelInfo?.connected ? `CONNECTED (Root: ${tunnelInfo.device_root})` : "DISCONNECTED"}
                                </span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-white/50">Filesystem Access:</span>
                                <span className={mountInfo?.mnt1_writable ? "text-green-400 font-bold" : "text-yellow-400 font-bold"}>
                                    {mountInfo ? mountInfo.stage_message : "READ ONLY"}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* SECTION 2: Core Patches */}
                    <div className={`bg-black/40 border border-white/10 rounded-xl p-5 backdrop-blur-xl flex flex-col gap-4 transition-opacity ${!mountInfo?.mnt1_writable ? 'opacity-50 pointer-events-none' : ''}`}>
                        <h2 className="text-lg font-bold text-white/80">2. Exploit Patches (Hello Bypass)</h2>
                        
                        <div className="flex flex-col gap-3">
                            <div className="grid grid-cols-[1fr_auto] gap-2">
                                <button 
                                    onClick={handleSetupAppPatch}
                                    disabled={!!loading}
                                    className="flex justify-between items-center bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-3 rounded-lg transition-colors"
                                >
                                    <div className="text-left">
                                        <div className="font-bold">Disable Setup.app</div>
                                        <div className="text-[10px] text-white/40">Renames binaries & patches PurpleBuddy</div>
                                    </div>
                                    <span className="text-red-400 text-lg">→</span>
                                </button>
                                <button 
                                    onClick={handleRestoreSetupApp}
                                    disabled={!!loading}
                                    className="bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-3 rounded-lg transition-colors text-white/60 hover:text-white"
                                    title="Restore Setup.app"
                                >
                                    ↺
                                </button>
                            </div>

                            <div className="grid grid-cols-[1fr_auto] gap-2">
                                <button 
                                    onClick={handleActivationPatch}
                                    disabled={!!loading}
                                    className="flex justify-between items-center bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-3 rounded-lg transition-colors"
                                >
                                    <div className="text-left">
                                        <div className="font-bold">Inject Activation Record</div>
                                        <div className="text-[10px] text-white/40">Writes raw data_ark payload</div>
                                    </div>
                                    <span className="text-red-400 text-lg">→</span>
                                </button>
                                {/* No restore button for activation record yet as it is just a file drop, a clean restore removes the jailbreak entirely */}
                            </div>

                            <div className="grid grid-cols-[1fr_auto] gap-2">
                                <button 
                                    onClick={handleLockdownPatch}
                                    disabled={!!loading}
                                    className="flex justify-between items-center bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-3 rounded-lg transition-colors"
                                >
                                    <div className="text-left">
                                        <div className="font-bold">Disable Lockdown Checks</div>
                                        <div className="text-[10px] text-white/40">Blocks ALBERT servers via /etc/hosts</div>
                                    </div>
                                    <span className="text-red-400 text-lg">→</span>
                                </button>
                                <button 
                                    onClick={handleRestoreLockdown}
                                    disabled={!!loading}
                                    className="bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-3 rounded-lg transition-colors text-white/60 hover:text-white"
                                    title="Restore Lockdown/Hosts"
                                >
                                    ↺
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* SECTION 3: Persistence Arsenal */}
                    <div className={`bg-black/40 border border-white/10 rounded-xl p-5 backdrop-blur-xl flex flex-col gap-4 transition-opacity ${!tunnelInfo?.connected ? 'opacity-50 pointer-events-none' : ''}`}>
                        <div className="flex justify-between items-center">
                            <h2 className="text-lg font-bold text-white/80">3. Persistence Engine</h2>
                            <button 
                                onClick={handleCheckTethered}
                                disabled={!!loading}
                                className="px-3 py-1 bg-white/10 hover:bg-white/20 rounded text-[10px] font-bold"
                            >
                                VALIDATE STATE
                            </button>
                        </div>

                        <div className="bg-white/5 p-3 rounded border border-white/5 flex flex-col gap-2 min-h-20">
                            {tetheredState ? (
                                <>
                                    <div className="flex justify-between">
                                        <span className="text-white/50">Bypass Active:</span>
                                        <span className={tetheredState.bypass_active ? "text-green-400" : "text-red-400"}>
                                            {tetheredState.bypass_active ? "YES" : "NO"}
                                        </span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-white/50">Reboot Safe (Untethered):</span>
                                        <span className={tetheredState.reboot_safe ? "text-green-400" : "text-red-400"}>
                                            {tetheredState.reboot_safe ? "YES" : "NO (TETHERED)"}
                                        </span>
                                    </div>
                                    <div className="mt-2 text-[10px] text-white/40">
                                        Active Patches: {tetheredState.current_patches.join(', ') || 'None'}
                                    </div>
                                </>
                            ) : (
                                <div className="text-white/30 text-center m-auto">No persistence state data. Run Validate.</div>
                            )}
                        </div>

                        <button 
                            onClick={handleInstallUntethered}
                            disabled={!!loading || !mountInfo?.mnt1_writable}
                            className="bg-red-900/20 hover:bg-red-900/40 border border-red-500/30 text-red-400 py-3 rounded-lg transition-colors font-bold disabled:opacity-50 mt-2 flex justify-center items-center gap-2"
                        >
                            <span>INSTALL UNTETHERED PERSISTENCE</span>
                            <span className="text-[10px] bg-red-500/20 px-2 py-0.5 rounded-full text-red-300 border border-red-500/30">RISKY</span>
                        </button>
                        
                        <button 
                            onClick={handleRemoveUntethered}
                            disabled={!!loading || !mountInfo?.mnt1_writable}
                            className="bg-white/5 hover:bg-white/10 border border-white/10 text-white/70 py-3 rounded-lg transition-colors font-bold disabled:opacity-50 flex justify-center items-center gap-2"
                        >
                            <span>REMOVE UNTETHERED PERSISTENCE</span>
                            <span className="text-[10px] bg-white/10 px-2 py-0.5 rounded-full text-white/50 border border-white/20">CLEAN UNINSTALL</span>
                        </button>
                    </div>

                </div>

                {/* Right Column: Terminal Logs */}
                <div className="bg-black border border-white/10 rounded-xl flex flex-col overflow-hidden relative shadow-2xl">
                    <div className="bg-white/5 border-b border-white/10 p-3 flex justify-between items-center shadow-md z-10">
                        <div className="flex items-center gap-3">
                            <div className="flex gap-1.5">
                                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                                <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                                <div className="w-3 h-3 rounded-full bg-green-500"></div>
                            </div>
                            <span className="font-bold text-white/70">Filesystem Terminal</span>
                        </div>
                        <div className="flex gap-2">
                            {loading && (
                                <span className="text-green-400 animate-pulse flex items-center gap-2 text-[10px]">
                                    <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                                    {loading}
                                </span>
                            )}
                            <button onClick={clearLogs} className="text-white/40 hover:text-white transition-colors">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"></path><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path></svg>
                            </button>
                        </div>
                    </div>
                    <div className="p-4 flex-1 overflow-y-auto whitespace-pre-wrap flex flex-col gap-1.5 text-[11px] bg-[#0A0A0A]">
                        {logs.length === 0 ? (
                            <div className="text-white/20 italic mt-auto">Awaiting command execution...</div>
                        ) : (
                            logs.map((log, i) => (
                                <div 
                                    key={i} 
                                    className={`font-mono ${
                                        log.includes('ERROR') || log.includes('❌') ? 'text-red-400' : 
                                        log.includes('✅') ? 'text-green-400' : 
                                        log.includes('⚠️') ? 'text-yellow-400' : 
                                        log.includes('╔══') || log.includes('╚══') || log.includes('║') ? 'text-blue-400' :
                                        'text-gray-300'
                                    }`}
                                >
                                    {log}
                                </div>
                            ))
                        )}
                        <div ref={logEndRef} />
                    </div>
                </div>
            </div>
        </div>
    );
};
