import { Settings } from "lucide-react";
import type { NavId, ResolvedNavItem } from "./types";

type BottomNavProps = {
  active: NavId;
  onNavigate: (id: NavId) => void;
  items: ResolvedNavItem[];
};

export function BottomNav({ active, onNavigate, items }: BottomNavProps) {
  return (
    <nav className="bottom-nav">
      <div className="bottom-nav-track">
        {items.map((item) => (
          <button
            key={item.id}
            type="button"
            title={item.title}
            aria-label={item.label}
            aria-disabled={item.disabled}
            tabIndex={item.disabled ? -1 : 0}
            className={`bottom-nav-item ${active === item.id ? "active" : ""} ${item.disabled ? "disabled" : ""}`}
            onClick={() => {
              if (!item.disabled) {
                onNavigate(item.id);
              }
            }}
          >
            <span className="bottom-nav-icon">
              {item.icon === "settings" ? <Settings className="nav-icon-svg" size={16} /> : item.icon}
            </span>
            <span className="visually-hidden">{item.label}</span>
          </button>
        ))}
      </div>
    </nav>
  );
}
