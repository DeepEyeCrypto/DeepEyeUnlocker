import React from 'react';
import './Sidebar.css';

interface SidebarProps {
  active: string;
  onSelect: (id: string) => void;
}

export function Sidebar({ active, onSelect }: SidebarProps) {
  const platforms = [
    { id: 'android',  label: 'Android',  icon: '🤖', color: '#00FF44' },
    { id: 'qualcomm', label: 'Qualcomm', icon: '⚡', color: '#FF3D00' },
    { id: 'apple',    label: 'Apple iOS',icon: '🍎', color: '#FFD700' },
    { id: 'samsung',  label: 'Samsung',  icon: '💠', color: '#1428A0' },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span className="logo-text">DEEPEYE</span>
        <span className="logo-badge">v1.0</span>
      </div>
      
      <nav className="sidebar-nav">
        {platforms.map(p => (
          <button
            key={p.id}
            className={`nav-item ${active === p.id ? 'active' : ''}`}
            style={{ '--platform-color': p.color } as React.CSSProperties}
            onClick={() => onSelect(p.id)}
          >
            <span className="nav-icon">{p.icon}</span>
            <span className="nav-label">{p.label}</span>
            {active === p.id && (
              <span className="nav-indicator" />
            )}
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="version-info">Build: 2026.04.18</div>
      </div>
    </aside>
  );
}
