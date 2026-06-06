import { OperationCard } from '../components/ui/OperationCard';
import { useOperationSessionStore } from '../stores/useOperationSessionStore';
import { Signal, Wifi, Key } from 'lucide-react';
import { OperationType } from '../lib/session-types';

export function ModesScreen() {
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
        <h2 className="text-3xl font-bold text-white tracking-tight">Activation Modes</h2>
        <p className="text-gray-400">Select the bypass method for the connected device.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <OperationCard
          title="Hello Signal"
          description="A12+ Full Signal Bypass. Requires registered IMEI and clean activation state. Untethered."
          icon={<Signal className="w-6 h-6 text-[#A78BFA]" />}
          tag="SAFE"
          onClick={() => handleAction('HelloActivation')}
          loading={isStarting}
        />

        <OperationCard
          title="Hello WiFi (No Signal)"
          description="Untethered bypass for devices without signal capability. Ideal for broken basebands or unsupported iOS."
          icon={<Wifi className="w-6 h-6 text-[#A78BFA]" />}
          tag="SAFE"
          onClick={() => handleAction('HelloNoSignalActivation')}
          loading={isStarting}
        />

        <OperationCard
          title="Passcode Backup"
          description="Extract activation tickets and lockdown records before restoring the device."
          icon={<Key className="w-6 h-6 text-[#A78BFA]" />}
          tag="ADVANCED"
          onClick={() => handleAction('PasscodeActivation')}
          loading={isStarting}
        />
      </div>
    </div>
  );
}
