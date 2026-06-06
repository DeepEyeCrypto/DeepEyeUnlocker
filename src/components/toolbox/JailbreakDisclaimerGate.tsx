import { useState } from 'react';

interface Props {
  onAccept: () => void;
}

export function JailbreakDisclaimerGate({ onAccept }: Props) {
  const [checked, setChecked] = useState(false);

  return (
    <div className="flex flex-col p-6 rounded-2xl bg-red-500/5 border border-red-500/20 max-w-2xl backdrop-blur-xl">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-xl">⚠️</span>
        <h3 className="text-lg font-bold text-red-400 uppercase tracking-widest">
          Jailbreak Tools
        </h3>
      </div>

      <p className="text-sm text-gray-300 leading-relaxed mb-4">
        These tools modify system-level security on the device. Use only on devices you own or are
        authorized to test. DeepEye is not responsible for any damage, boot loops, or data loss that
        may occur.
      </p>

      <label className="flex items-center gap-3 mb-6 cursor-pointer group">
        <div className="relative flex items-center justify-center w-5 h-5 border-2 border-red-500/40 rounded bg-red-500/10 group-hover:border-red-500/60 transition-colors">
          <input
            type="checkbox"
            className="absolute opacity-0 w-full h-full cursor-pointer"
            checked={checked}
            onChange={(e) => setChecked(e.target.checked)}
          />
          {checked && <span className="text-red-400 text-xs">✓</span>}
        </div>
        <span className="text-sm font-semibold text-gray-300 group-hover:text-white transition-colors">
          I understand the risks and accept responsibility
        </span>
      </label>

      <button
        onClick={onAccept}
        disabled={!checked}
        className="self-start px-6 py-2.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 font-semibold rounded-lg transition-colors border border-red-500/20 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Show jailbreak tools
      </button>
    </div>
  );
}
