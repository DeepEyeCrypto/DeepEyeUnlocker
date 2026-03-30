import { NAV_ITEMS, type NavId } from "./types";

type BottomNavProps = {
  active: NavId;
  onNavigate: (id: NavId) => void;
};

export function BottomNav({ active, onNavigate }: BottomNavProps) {
  return (
    <nav className="bottom-nav">
      {NAV_ITEMS.slice(0, 5).map((item) => (
        <button
          key={item.id}
          className={`bottom-nav-item ${active === item.id ? "active" : ""}`}
          onClick={() => onNavigate(item.id)}
        >
          <span className="bottom-nav-icon">{item.icon}</span>
          <span className="bottom-nav-label">{item.label}</span>
        </button>
      ))}
    </nav>
  );
}

