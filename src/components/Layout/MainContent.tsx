import type { ReactNode } from "react";

export function MainContent({ children }: { children: ReactNode }) {
  return <main className="main-content">{children}</main>;
}

