import { getPlatform } from "../lib/platform";
import { SideNav } from "./Layout/SideNav";
import { getNavItems, type NavId } from "./Layout/types";

interface Props {
  active: string;
  onSelect: (id: string) => void;
}

export default function Sidebar({ active, onSelect }: Props) {
  const items = getNavItems(getPlatform());

  return (
    <SideNav
      active={active as NavId}
      onNavigate={(id) => onSelect(id)}
      items={items}
    />
  );
}
