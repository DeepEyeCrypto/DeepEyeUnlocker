import { ReactNode } from 'react';
import { CheckCircle2 } from 'lucide-react';

interface OperationCardProps {
  title: string;
  description: string;
  icon: ReactNode;
  tag?: 'SAFE' | 'RISKY' | 'ADVANCED';
  onClick: () => void;
  loading?: boolean;
}

export function OperationCard({
  title,
  description,
  icon,
  tag,
  onClick,
  loading,
}: OperationCardProps) {
  return (
    <div className="relative p-5 rounded-xl bg-white/5 border border-white/10 backdrop-blur-md transition-all duration-300 hover:bg-white/10 hover:shadow-[0_0_20px_rgba(124,58,237,0.15)] hover:-translate-y-1 flex flex-col h-full group">
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-white/5 rounded-lg border border-white/10 group-hover:border-[#A78BFA]/50 transition-colors">
            {icon}
          </div>
          <h3 className="text-lg font-semibold text-white tracking-wide">{title}</h3>
        </div>
        {tag && (
          <span
            className={`px-2 py-1 text-xs font-medium rounded-full border tracking-wider ${
              tag === 'SAFE'
                ? 'bg-green-500/10 text-green-400 border-green-500/20'
                : tag === 'RISKY'
                  ? 'bg-red-500/10 text-red-400 border-red-500/20'
                  : 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
            }`}
          >
            {tag}
          </span>
        )}
      </div>

      <p className="text-sm text-gray-400 mb-6 flex-grow leading-relaxed">{description}</p>

      <button
        onClick={onClick}
        disabled={loading}
        className="w-full py-2.5 rounded-lg bg-gradient-to-r from-[#7C3AED] to-[#A78BFA] text-white font-medium hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center gap-2 shadow-[0_4px_14px_rgba(124,58,237,0.3)] hover:shadow-[0_6px_20px_rgba(124,58,237,0.4)]"
      >
        {loading ? (
          <>
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span>RUNNING...</span>
          </>
        ) : (
          <>
            <CheckCircle2 className="w-4 h-4" />
            <span>RUN BYPASS</span>
          </>
        )}
      </button>
    </div>
  );
}
