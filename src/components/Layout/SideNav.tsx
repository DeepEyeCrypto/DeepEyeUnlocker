import { Settings, Terminal } from "lucide-react";
import { useState } from "react";
import type { NavId, ResolvedNavItem } from "./types";

type SideNavProps = {
  active: NavId;
  onNavigate: (id: NavId) => void;
  items: ResolvedNavItem[];
};

export function SideNav({ active, onNavigate, items }: SideNavProps) {
  const [collapsed, setCollapsed] = useState(false);

  const renderIcon = (icon: string) => {
    if (icon === "settings") {
      return <Settings className="nav-icon-svg" size={16} />;
    }

    if (icon === "terminal") {
      return <Terminal className="nav-icon-svg" size={16} />;
    }

    return icon;
  };

  return (
    <nav className={`sidenav ${collapsed ? "collapsed" : ""}`}>
      <button className="sidenav-toggle" onClick={() => setCollapsed((v) => !v)}>
        {collapsed ? "›" : "‹"}
      </button>
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          title={item.title}
          aria-disabled={item.disabled}
          tabIndex={item.disabled ? -1 : 0}
          className={`sidenav-item ${active === item.id ? "active" : ""} ${item.disabled ? "disabled" : ""}`}
          onClick={() => {
            if (!item.disabled) {
              onNavigate(item.id);
            }
          }}
        >
          <span className="sidenav-icon">
            {renderIcon(item.icon)}
          </span>
          {!collapsed && (
            <span className="sidenav-meta">
              <span className="sidenav-label">{item.label}</span>
              {item.disabled && item.badge && <span className="nav-badge">{item.badge}</span>}
            </span>
          )}
        </button>
      ))}
    </nav>
  );
}
