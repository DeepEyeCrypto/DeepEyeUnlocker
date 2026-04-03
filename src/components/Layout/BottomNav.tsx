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
            aria-disabled={item.disabled}
            tabIndex={item.disabled ? -1 : 0}
            className={`bottom-nav-item ${active === item.id ? "active" : ""} ${item.disabled ? "disabled" : ""}`}
            onClick={() => {
              if (!item.disabled) {
                onNavigate(item.id);
              }
            }}
          >
            <span className="bottom-nav-icon">{item.icon}</span>
            <span className="bottom-nav-meta">
              <span className="bottom-nav-label">{item.label}</span>
              {item.disabled && item.badge && <span className="nav-badge">{item.badge}</span>}
            </span>
          </button>
        ))}
      </div>
    </nav>
  );
}
