export function ToolboxSectionHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-4 pb-2 border-b border-white/10 mt-10">
      <h2 className="text-sm font-bold text-gray-300 uppercase tracking-widest">{title}</h2>
      {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
    </div>
  );
}
