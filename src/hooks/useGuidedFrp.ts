import { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import type { ConnectedDevice } from '../lib/devices';
import type { RoutingResult } from './useDeviceDB';

export type FrpStep = 'SELECT' | 'GUIDE' | 'CONNECT' | 'ERASE' | 'COMPLETE';

export const useGuidedFrp = () => {
    const [step, setStep] = useState<FrpStep>('SELECT');
    const [result, setResult] = useState<RoutingResult | null>(null);
    const [connected, setConnected] = useState(false);
    const [progress, setProgress] = useState(0);
    const [error, setError] = useState<string | null>(null);
    const [isExecuting, setIsExecuting] = useState(false);

    // Poll for device connection when in CONNECT step
    useEffect(() => {
        let timer: number | undefined;
        if (step === 'CONNECT' && result) {
            timer = window.setInterval(async () => {
                try {
                    const devices = await invoke<ConnectedDevice[]>('get_connected_devices');
                    if (devices.length > 0) {
                        // Protocol matching logic
                        const isMatched = devices.some(d => {
                            const mode = d.mode.toLowerCase();
                            const protocol = result.protocol.toLowerCase();
                            
                            if (protocol === 'edl' && mode.includes('edl')) return true;
                            if (protocol === 'mtkbrom' && (mode.includes('brom') || mode.includes('da'))) return true;
                            if (protocol === 'adb' && mode.includes('adb')) return true;
                            if (protocol === 'samsungodin' && (mode.includes('odin') || mode.includes('download'))) return true;
                            if (protocol === 'fastboot' && mode.includes('fastboot')) return true;
                            
                            return false;
                        });

                        if (isMatched) {
                            setConnected(true);
                            setStep('ERASE');
                        }
                    }
                } catch (pollError: unknown) {
                    console.error("Poll error:", pollError);
                }
            }, 1500);
        }
        return () => {
            if (timer !== undefined) {
                clearInterval(timer);
            }
        };
    }, [step, result]);

    // Listen for backend progress events
    useEffect(() => {
        const unlistenProgress = listen<number>('frp-progress', (event) => {
            setProgress(event.payload);
        });
        const unlistenComplete = listen<boolean>('frp-complete', () => {
            setStep('COMPLETE');
            setIsExecuting(false);
        });

        return () => {
            unlistenProgress.then(u => u());
            unlistenComplete.then(u => u());
        };
    }, []);

    const startGuided = (res: RoutingResult) => {
        setResult(res);
        setStep('GUIDE');
    };

    const confirmGuide = () => {
        setStep('CONNECT');
    };

    const executeErase = async () => {
        if (!result) return;
        setIsExecuting(true);
        setError(null);
        try {
            await invoke('frp_execute_protocol', {
                protocol: result.protocol,
                partitions: result.frp_partitions,
            });
        } catch (error: unknown) {
            setError(String(error));
            setIsExecuting(false);
        }
    };

    const reset = () => {
        setStep('SELECT');
        setResult(null);
        setConnected(false);
        setProgress(0);
        setError(null);
        setIsExecuting(false);
    };

    return {
        step,
        result,
        connected,
        progress,
        error,
        isExecuting,
        startGuided,
        confirmGuide,
        executeErase,
        reset,
    };
};
