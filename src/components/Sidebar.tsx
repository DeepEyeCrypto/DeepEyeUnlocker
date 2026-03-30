import { SideNav } from "./Layout/SideNav";
import type { NavId } from "./Layout/types";

interface Props {
  active: string;
  onSelect: (id: string) => void;
}

export default function Sidebar({ active, onSelect }: Props) {
  return (
    <SideNav
      active={active as NavId}
      onNavigate={(id) => onSelect(id)}
    />
  );
}
