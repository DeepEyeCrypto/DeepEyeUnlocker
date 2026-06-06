import { useState } from 'react';
import { Key } from 'lucide-react';

interface LicenseKeyInputProps {
  onActivate: (key: string) => Promise<void>;
  isLoading: boolean;
}

export function LicenseKeyInput({ onActivate, isLoading }: LicenseKeyInputProps) {
  const [key, setKey] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!key.trim()) return;

    // Basic format check
    if (!/^[A-Za-z0-9\-]{20,40}$/.test(key.trim())) {
      setError('Invalid license key format');
      return;
    }

    try {
      setError(null);
      await onActivate(key.trim());
      setKey('');
    } catch (err: any) {
      setError(err.toString());
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3">
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Key className="h-5 w-5 text-gray-400" />
        </div>
        <input
          type="password"
          value={key}
          onChange={(e) => {
            setKey(e.target.value);
            setError(null);
          }}
          className="block w-full pl-10 pr-3 py-2 border border-white/10 rounded-xl bg-black/50 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#7C3AED] focus:border-transparent transition-colors sm:text-sm font-mono"
          placeholder="PRO-XXXX-XXXX-XXXX-XXXX"
          disabled={isLoading}
        />
      </div>
      {error && <span className="text-red-400 text-xs">{error}</span>}
      <button
        type="submit"
        disabled={isLoading || !key.trim()}
        className="self-start px-4 py-2 bg-[#7C3AED] hover:bg-[#6D28D9] text-white text-sm font-semibold rounded-lg disabled:opacity-50 transition-colors"
      >
        {isLoading ? 'Activating...' : 'Activate License'}
      </button>
    </form>
  );
}
