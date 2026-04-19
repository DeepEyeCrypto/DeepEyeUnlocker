import React from 'react';
import type { NavigationItem, NavigationSection, WorkspaceId } from '../../lib/desktopWorkspace';
import './Sidebar.css';

interface SidebarProps {
  active: WorkspaceId;
  items: NavigationItem[];
  connectedCount: number;
  featureCount: number;
  onSelect: (id: WorkspaceId) => void;
}

const SECTION_TITLES: Record<NavigationSection, string> = {
  overview: 'Overview',
  labs: 'Desktop Labs',
  system: 'System',
};

export function Sidebar({ active, items, connectedCount, featureCount, onSelect }: SidebarProps) {
  const sections: NavigationSection[] = ['overview', 'labs', 'system'];

  return (
    <aside className="sidebar" data-tauri-drag-region>
      <div className="sidebar-logo">
        <div>
          <span className="logo-text">DEEPEYE</span>
          <span className="logo-tagline">Integrated Desktop Workspace</span>
        </div>
        <span className="logo-badge">v{__APP_VERSION__}</span>
      </div>

      <div className="sidebar-summary glass">
        <div>
          <span className="sidebar-summary__value">{connectedCount}</span>
          <span className="sidebar-summary__label">live devices</span>
        </div>
        <div>
          <span className="sidebar-summary__value">{featureCount}</span>
          <span className="sidebar-summary__label">mapped Kotlin ops</span>
        </div>
      </div>
      
      <nav className="sidebar-nav">
        {sections.map((section) => {
          const sectionItems = items.filter((item) => item.section === section);
          if (sectionItems.length === 0) {
            return null;
          }

          return (
            <div key={section} className="sidebar-section">
              <div className="sidebar-section__title">{SECTION_TITLES[section]}</div>

              <div className="sidebar-section__items">
                {sectionItems.map((item) => (
                  <button
                    key={item.id}
                    className={`nav-item ${active === item.id ? 'active' : ''}`}
                    style={{ '--platform-color': item.color } as React.CSSProperties}
                    onClick={() => onSelect(item.id)}
                  >
                    <div className="nav-item__main">
                      <span className="nav-icon">{item.icon}</span>
                      <div className="nav-copy">
                        <span className="nav-eyebrow">{item.eyebrow}</span>
                        <span className="nav-label">{item.label}</span>
                        <span className="nav-description">{item.description}</span>
                      </div>
                    </div>

                    <div className="nav-item__meta">
                      {item.badge && <span className="nav-badge">{item.badge}</span>}
                      {active === item.id && <span className="nav-indicator" />}
                    </div>
                  </button>
                ))}
              </div>
            </div>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="version-info">Build: {__APP_VERSION__}</div>
        <div className="version-info version-info--muted">Desktop labs synced with Kotlin feature inventory</div>
      </div>
    </aside>
  );
}
