import { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';
import {
    ChevronRight,
    Cpu,
    Layers3,
    LoaderCircle,
    LocateFixed,
    ShieldCheck,
    TriangleAlert,
    Zap,
} from 'lucide-react';
import { useGuidedFrp } from '../../hooks/useGuidedFrp';
import { useDeviceDb, type DeviceEntry, type RoutingResult } from '../../hooks/useDeviceDB';
import './guided-frp.css';

const GuidedFRP = () => {
    const { 
        step, 
        result, 
        progress, 
        error, 
        isExecuting, 
        startGuided, 
        confirmGuide, 
        executeErase, 
        reset 
    } = useGuidedFrp();
    
    const { searchDevices } = useDeviceDb();
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<DeviceEntry[]>([]);

    const handleSearch = async (q: string) => {
        setSearchQuery(q);
        if (q.length > 2) {
            const results = await searchDevices(q);
            setSearchResults(results);
        } else {
            setSearchResults([]);
        }
    };

    const renderHeader = () => (
        <div className="guided-header">
            <div className="guided-title-area">
                <ShieldCheck className="title-icon animate-pulse" />
                <h1>Guided FRP Bypass</h1>
            </div>
            <p className="subtitle">Intelligence-driven, safety-first workflow for partition erasure.</p>
            
            <div className="step-indicator">
                {['Select', 'Guide', 'Connect', 'Erase', 'Done'].map((s, i) => {
                    const stepNames = ['SELECT', 'GUIDE', 'CONNECT', 'ERASE', 'COMPLETE'];
                    const active = step === stepNames[i];
                    const visited = stepNames.indexOf(step) > i;
                    return (
                        <div key={s} className={`step-dot ${active ? 'active' : ''} ${visited ? 'visited' : ''}`}>
                            <div className="dot-inner">{visited ? '✓' : i + 1}</div>
                            <span>{s}</span>
                        </div>
                    );
                })}
            </div>
        </div>
    );

    const renderStepSelect = () => (
        <div className="step-content select-step">
            <div className="search-box">
                <input 
                    type="text" 
                    placeholder="Search model (e.g., Redmi Note 11)..." 
                    value={searchQuery}
                    onChange={(e) => handleSearch(e.target.value)}
                    autoFocus
                />
            </div>
            
            <div className="search-results scrollbar-hide">
                {searchResults.map((device) => (
                    <div 
                        key={`${device.brand}-${device.model}`} 
                        className="device-card-premium"
                        onClick={async () => {
                            // We need a RoutingResult here. 
                            // For simplicity, let's trigger autoRoute via hook
                            const res = await invokeAutoRoute(device.model);
                            if (res) startGuided(res);
                        }}
                    >
                        <div className="card-left">
                            <Cpu className="chip-icon" />
                            <div className="name-area">
                                <span className="brand">{device.brand}</span>
                                <span className="model">{device.model}</span>
                            </div>
                        </div>
                        <div className="card-right">
                            <span className="badge amber">{device.soc_family}</span>
                            <span className="badge blue">{device.protocol}</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );

    // Helper for auto_route invocation
    const invokeAutoRoute = async (model: string): Promise<RoutingResult | null> => {
        try {
            const res = await invoke<RoutingResult>('db_auto_route', { model });
            return res;
        } catch (error: unknown) {
            console.error(error);
            return null;
        }
    };

    const renderStepGuide = () => (
        <div className="step-content guide-step">
            <div className="guide-layout">
                <div className="guide-text">
                    <div className="mode-badge">{result?.hardware_guide.mode_name}</div>
                    <h2>How to enter mode:</h2>
                    <ul className="guide-steps">
                        {result?.hardware_guide.steps.map((s: string, i: number) => (
                            <li key={i}>
                                <span className="step-num">{i + 1}</span>
                                {s}
                            </li>
                        ))}
                    </ul>
                    
                    <div className="combo-box">
                            <Layers3 className="combo-icon" />
                        <div>
                            <div className="label">Recommended Combo</div>
                            <div className="value">{result?.hardware_guide.button_combo}</div>
                        </div>
                    </div>
                </div>

                <div className="guide-visual">
                    {result?.hardware_guide.test_point ? (
                        <div className="tp-container fade-in">
                            <img 
                                src={`/src/assets/tp/${result?.hardware_guide.test_point}.png`} 
                                alt="Test Point Map"
                                onError={(e) => {
                                    (e.target as HTMLImageElement).src = '/src/assets/tp/generic_tp.png';
                                    (e.target as HTMLImageElement).onerror = null;
                                }}
                                className="tp-image"
                            />
                            <div className="tp-overlay">
                                <LocateFixed className="tp-badge-icon" />
                                <span>REPAIR MODE : Intel Active</span>
                            </div>
                        </div>
                    ) : (
                        <div className="generic-visual fade-in">
                            <div className="phone-outline">
                                <div className="buttons-vol">
                                    <div className={`active-btn ${result?.hardware_guide.button_combo.includes('VOL') ? 'pulse' : ''}`}></div>
                                </div>
                                <div className="buttons-pwr">
                                    <div className={`active-btn ${result?.hardware_guide.button_combo.includes('PWR') ? 'pulse' : ''}`}></div>
                                </div>
                            </div>
                            <div className="usb-flow">
                                <div className="usb-plug pulse"></div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
            
            {result?.hardware_guide.warning && (
                <div className="safety-warning glow-amber">
                    <TriangleAlert className="warn-icon" />
                    <span>{result.hardware_guide.warning}</span>
                </div>
            )}

            <div className="action-row">
                <button className="btn-secondary" onClick={reset}>Back</button>
                <button className="btn-primary glow-blue" onClick={confirmGuide}>
                    I've connected the device
                    <ChevronRight className="icon-r" />
                </button>
            </div>
        </div>
    );

    const renderStepConnect = () => (
        <div className="step-content connect-step">
            <div className="scanning-container">
                <div className="radar">
                    <div className="circle"></div>
                    <div className="circle"></div>
                    <div className="circle"></div>
                    <div className="line"></div>
                </div>
                <h3>Waiting for Hardware Connection...</h3>
                <p>Plug in the device in <strong>{result?.hardware_guide.mode_name}</strong>.</p>
                <div className="connection-info">
                   <div className="pill">Protocol: {result?.protocol}</div>
                   <div className="pill">Strategy: {result?.strategy}</div>
                </div>
            </div>
            <button className="btn-text" onClick={reset}>Cancel</button>
        </div>
    );

    const renderStepErase = () => (
        <div className="step-content erase-step">
            <div className="danger-header">
                <TriangleAlert className="danger-icon animate-bounce" />
                <h2>Ready for Partition Erasure</h2>
                <p>Connected: {result?.device?.model} on {result?.protocol}</p>
            </div>

            <div className="partition-list">
                <div className="list-title">Target Partitions:</div>
                {result?.frp_partitions.map((p: string) => (
                    <div key={p} className="part-item">
                        <Zap className="p-icon" />
                        <span>{p}</span>
                    </div>
                ))}
            </div>

            {result?.danger_zone && (
                <div className="danger-alert">
                    This operation cannot be undone. System stability may be affected if interrupted.
                </div>
            )}

            <div className="progress-area">
                <div className="progress-bar-bg">
                    <div className="progress-fill" style={{ width: `${progress}%` }}></div>
                </div>
                <div className="progress-stats">
                    <span>{isExecuting ? 'Erasing...' : 'Ready'}</span>
                    <span>{progress}%</span>
                </div>
            </div>

            {error && <div className="error-toast">{error}</div>}

            <div className="action-row">
                <button className="btn-secondary" onClick={reset} disabled={isExecuting}>Cancel</button>
                <button 
                    className={`btn-primary ${isExecuting ? 'loading' : 'glow-red'}`} 
                    onClick={executeErase}
                    disabled={isExecuting}
                >
                    {isExecuting ? <LoaderCircle className="spin" /> : 'CONFIRM & ERASE'}
                </button>
            </div>
        </div>
    );

    const renderStepComplete = () => (
        <div className="step-content complete-step fade-in">
            <div className="success-check">✓</div>
            <h2>FRP Bypass Successful</h2>
            <p>Partition(s) erased. You can now reboot the device.</p>
            <div className="complete-actions">
                <button className="btn-primary glow-green" onClick={reset}>Done</button>
            </div>
        </div>
    );

    return (
        <div className="guided-frp-page deepeye-glass">
            {renderHeader()}
            <div className="workflow-container">
                {step === 'SELECT' && renderStepSelect()}
                {step === 'GUIDE' && renderStepGuide()}
                {step === 'CONNECT' && renderStepConnect()}
                {step === 'ERASE' && renderStepErase()}
                {step === 'COMPLETE' && renderStepComplete()}
            </div>
        </div>
    );
};

export default GuidedFRP;
