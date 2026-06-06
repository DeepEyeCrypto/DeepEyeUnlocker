import { ToolboxActionCard } from '../components/ui/ToolboxActionCard';
import { Search, KeyRound, UserMinus } from 'lucide-react';
import { useOperationSessionStore } from '../stores/useOperationSessionStore';
import { OperationType } from '../lib/session-types';

export function FmiScreen() {
  const startSession = useOperationSessionStore((state) => state.startSession);
  const isStarting = useOperationSessionStore((state) => state.isStarting);

  const handleAction = async (opType: OperationType) => {
    try {
      await startSession(opType);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="flex flex-col gap-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex flex-col gap-2">
        <h2 className="text-3xl font-bold text-white tracking-tight">FMI / Account</h2>
        <p className="text-gray-400">
          Tools for managing activation lock, iCloud queries, and token extraction.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <ToolboxActionCard
          title="FMI OFF Check (SN/IMEI)"
          description="Query Apple GSX servers to check real-time Find My iPhone status."
          icon={<Search className="w-6 h-6 text-white" />}
          tag="SAFE"
          onClick={() => handleAction('FmiOff')}
          disabled={isStarting}
        />

        <ToolboxActionCard
          title="Extract Auth Token"
          description="Pull Accounts3.sqlite and extract raw iCloud authentication token (Requires Open Menu)."
          icon={<KeyRound className="w-6 h-6 text-white" />}
          tag="PREMIUM"
          onClick={() => handleAction({ customCommand: 'extract_token' })}
          disabled={isStarting}
        />

        <ToolboxActionCard
          title="Remove Local Account"
          description="Force remove iCloud account from local filesystem. Does not turn off server-side FMI."
          icon={<UserMinus className="w-6 h-6 text-yellow-400" />}
          tag="WARNING"
          onClick={() => handleAction({ customCommand: 'remove_account' })}
          disabled={isStarting}
        />
      </div>
    </div>
  );
}
