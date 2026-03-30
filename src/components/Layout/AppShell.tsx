import type { ReactNode } from "react";
import { getPlatform, isMobile } from "../../lib/platform";
import { BottomNav } from "./BottomNav";
import { MainContent } from "./MainContent";
import { SideNav } from "./SideNav";
import { TitleBar } from "./TitleBar";
import type { NavId } from "./types";

type AppShellProps = {
  active: NavId;
  onNavigate: (id: NavId) => void;
  children: ReactNode;
};

export function AppShell({ active, onNavigate, children }: AppShellProps) {
  const mobile = isMobile();
  const platform = getPlatform();

  return (
    <div className="app-shell" data-layout={mobile ? "mobile" : "desktop"}>
      {platform === "windows" && <TitleBar />}
      <div className="app-body">
        {!mobile && <SideNav active={active} onNavigate={onNavigate} />}
        <MainContent>{children}</MainContent>
      </div>
      {mobile && <BottomNav active={active} onNavigate={onNavigate} />}
    </div>
  );
}

