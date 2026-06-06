import { useState } from 'react';
import { Settings } from 'lucide-react';
import { useDeviceStatusStore } from '../../stores/useDeviceStatusStore';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { ToolCard } from './ToolCard';
import { DfuAssistantWizard } from './DfuAssistantWizard';

export function DfuAssistantCard() {
  const [wizardOpen, setWizardOpen] = useState(false);

  const device = useDeviceStatusStore((s) => s.currentDevice);
  const isSessionActive = useOperationSessionStore((s) => s.activeSession !== null);

  const disabled = !device || isSessionActive;
  let disabledReason = '';
  if (!device) disabledReason = 'No device connected';
  else if (isSessionActive) disabledReason = 'Session in progress';

  return (
    <>
      <ToolCard
        title="DFU Assistant"
        description="Guide device into pwned DFU state using checkm8 exploit chain. Required for ramdisk booting."
        operationType="DfuAssist"
        requiredMode="any"
        riskLevel="medium"
        requiresConfirm={false}
        disabled={disabled}
        disabledReason={disabledReason}
        icon={<Settings className="w-5 h-5 text-gray-300" />}
        customAction={() => setWizardOpen(true)}
      />

      {wizardOpen && device && (
        <DfuAssistantWizard
          deviceModel={device.model || 'Unknown Device'}
          onSuccess={() => setWizardOpen(false)}
          onCancel={() => setWizardOpen(false)}
        />
      )}
    </>
  );
}
