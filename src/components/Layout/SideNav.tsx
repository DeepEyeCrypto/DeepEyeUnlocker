import { useState } from "react";
import { NAV_ITEMS, type NavId } from "./types";

type SideNavProps = {
  active: NavId;
  onNavigate: (id: NavId) => void;
};

export function SideNav({ active, onNavigate }: SideNavProps) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <nav className={`sidenav ${collapsed ? "collapsed" : ""}`}>
      <button className="sidenav-toggle" onClick={() => setCollapsed((v) => !v)}>
        {collapsed ? "›" : "‹"}
      </button>
      {NAV_ITEMS.map((item) => (
        <button
          key={item.id}
          className={`sidenav-item ${active === item.id ? "active" : ""}`}
          onClick={() => onNavigate(item.id)}
        >
          <span className="sidenav-icon">{item.icon}</span>
          {!collapsed && <span className="sidenav-label">{item.label}</span>}
        </button>
      ))}
    </nav>
  );
}

