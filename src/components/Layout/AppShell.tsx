import { useEffect, useMemo, type ReactNode } from "react";
import { getPlatform, isMobile } from "../../lib/platform";
import { BottomNav } from "./BottomNav";
import { MainContent } from "./MainContent";
import { SideNav } from "./SideNav";
import { TitleBar } from "./TitleBar";
import { getNavItems, type NavId } from "./types";

type AppShellProps = {
  active: NavId;
  onNavigate: (id: NavId) => void;
  children: ReactNode;
};

export function AppShell({ active, onNavigate, children }: AppShellProps) {
  const mobile = isMobile();
  const platform = getPlatform();
  const navItems = useMemo(() => getNavItems(platform), [platform]);

  useEffect(() => {
    const activeItem = navItems.find((item) => item.id === active);
    if (!activeItem || activeItem.disabled) {
      onNavigate("dashboard");
    }
  }, [active, navItems, onNavigate]);

  return (
    <div className="app-shell" data-layout={mobile ? "mobile" : "desktop"}>
      {platform === "windows" && <TitleBar />}
      <div className="app-body">
        {!mobile && <SideNav active={active} onNavigate={onNavigate} items={navItems} />}
        <MainContent>{children}</MainContent>
      </div>
      {mobile && <BottomNav active={active} onNavigate={onNavigate} items={navItems} />}
    </div>
  );
}
