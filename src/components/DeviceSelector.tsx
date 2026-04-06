import React, { useState, useMemo } from 'react';
import { DeviceEntry, getBrands, getModelsByBrand, getSupportBadge } from '../lib/devices';

interface DeviceSelectorProps {
  onSelect: (device: DeviceEntry) => void;
  selectedDevice: DeviceEntry | null;
  filterSupport?: import('../lib/devices').BromSupport[];
}

export function DeviceSelector({ onSelect, selectedDevice, filterSupport }: DeviceSelectorProps) {
  const [brand, setBrand] = useState<string>('');
  
  const filtered = useMemo(() => {
    // Import dynamically or assume it's part of devices.ts
    // Wait, allDevices is not imported directly. 
    // Let's modify the models array fetching directly using getModelsByBrand.
    return getModelsByBrand(brand).filter(d => filterSupport ? filterSupport.includes(d.brom_support) : true);
  }, [brand, filterSupport]);

  const brands = useMemo(() => getBrands(), []);
  const models = filtered;

  const handleBrandChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setBrand(e.target.value);
  };

  const handleModelChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const model = models.find(m => m.model === e.target.value);
    if (model) {
      onSelect(model);
    }
  };

  return (
    <div className="card glass glass-hover" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', padding: '1.5rem', marginBottom: '1.5rem' }}>
      <h3 style={{ margin: 0, fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-1)' }}>
        SELECT YOUR DEVICE
      </h3>
      
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
        <select 
          className="input glass-input" 
          value={brand} 
          onChange={handleBrandChange}
          style={{ width: '100%', padding: '0.75rem', borderRadius: '8px' }}
        >
          <option value="">Select Brand...</option>
          {brands.map(b => (
            <option key={b} value={b}>{b}</option>
          ))}
        </select>
        
        <select 
          className="input glass-input" 
          value={selectedDevice?.model || ''} 
          onChange={handleModelChange}
          disabled={!brand}
          style={{ width: '100%', padding: '0.75rem', borderRadius: '8px' }}
        >
          <option value="">Select Model...</option>
          {models.map(m => (
            <option key={m.model} value={m.model}>{m.model}</option>
          ))}
        </select>
      </div>

      {selectedDevice && (
        <div style={{ 
          marginTop: '0.5rem', 
          padding: '1rem', 
          borderRadius: '8px', 
          background: 'rgba(255, 255, 255, 0.03)',
          border: '1px solid rgba(255, 255, 255, 0.05)'
        }}>
          <h4 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-1)' }}>
            {selectedDevice.brand} {selectedDevice.model}
          </h4>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem', fontSize: '0.875rem', color: 'var(--text-2)' }}>
            <span>{selectedDevice.year}</span>
            <span>·</span>
            <span style={{ textTransform: 'capitalize' }}>{selectedDevice.type}</span>
            <span>·</span>
            <span>{selectedDevice.chipset}</span>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ 
              display: 'inline-flex', 
              alignItems: 'center', 
              gap: '0.375rem',
              padding: '0.25rem 0.75rem', 
              borderRadius: '9999px', 
              background: `${getSupportBadge(selectedDevice.brom_support).color}20`,
              color: getSupportBadge(selectedDevice.brom_support).color,
              fontSize: '0.875rem',
              fontWeight: 500
            }}>
              <div style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'currentColor' }} />
              {getSupportBadge(selectedDevice.brom_support).label}
            </span>
          </div>

          {selectedDevice.brom_support === 'none' && (
            <div style={{ marginTop: '1rem', color: 'var(--error, #ef4444)', fontSize: '0.875rem' }}>
              Warning: This device is not supported for BROM operations.
            </div>
          )}
          {selectedDevice.brom_support === 'edl_only' && (
            <div style={{ marginTop: '1rem', color: 'var(--primary, #3b82f6)', fontSize: '0.875rem' }}>
              Info: Use Qualcomm EDL mode for this device.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
