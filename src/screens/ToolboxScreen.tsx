import { useState } from 'react';
import { Shield, ShieldAlert, Zap, Cpu, Key, Play } from 'lucide-react';
import { useDeviceStatusStore } from '../stores/useDeviceStatusStore';
import { ToolboxSectionHeader } from '../components/toolbox/ToolboxSectionHeader';
import { ToolCardGrid } from '../components/toolbox/ToolCardGrid';
import { ToolCard } from '../components/toolbox/ToolCard';
import { DfuAssistantCard } from '../components/toolbox/DfuAssistantCard';
import { DiagnosticsCard } from '../components/toolbox/DiagnosticsCard';
import { JailbreakDisclaimerGate } from '../components/toolbox/JailbreakDisclaimerGate';
import { ToolboxNoDeviceBanner } from '../components/toolbox/ToolboxNoDeviceBanner';
import { useToolCapability } from '../hooks/useToolCapability';
import { isJailbreakCompatible } from '../lib/jailbreak-compat';

export function ToolboxScreen() {
  const device = useDeviceStatusStore((s) => s.currentDevice);
  const [jailbreakAccepted, setJailbreakAccepted] = useState(false);

  // Gating hooks
  const enterRecovery = useToolCapability('RecoveryEnter', ['normal']);
  const exitRecovery = useToolCapability('RecoveryExit', 'recovery');
  const exitDfu = useToolCapability('DfuExit', 'dfu');
  const purpleMode = useToolCapability('PurpleModeEntry', 'any');
  const rebootHello = useToolCapability('RebootToHello', ['normal']);
  const reboot = useToolCapability('Reboot', 'any');

  const bootFilesActivation = useToolCapability('BootFilesActivation', 'boot_files');
  const bootFilesBackup = useToolCapability('BootFilesBackup', 'boot_files');

  const otaBlock = useToolCapability('OtaBlock', ['normal']);
  const restoreBlock = useToolCapability('RestoreBlock', ['normal']);

  const palera1nCap = useToolCapability('JailbreakPalera1n', ['normal', 'recovery']);
  const checkra1nCap = useToolCapability('JailbreakCheckra1n', ['normal', 'recovery']);

  const palera1nCompat = isJailbreakCompatible('JailbreakPalera1n', device?.osVersion);
  const checkra1nCompat = isJailbreakCompatible('JailbreakCheckra1n', device?.osVersion);

  return (
    <div className="flex flex-col gap-6 animate-in fade-in slide-in-from-bottom-4 duration-500 pb-12">
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <h2 className="text-3xl font-bold text-white tracking-tight">Toolbox</h2>
          {device && (
            <div className="flex items-center gap-2 px-3 py-1.5 bg-blue-500/10 border border-blue-500/20 rounded-full">
              <span className="w-2 h-2 rounded-full bg-blue-400 animate-pulse" />
              <span className="text-xs font-semibold text-blue-400 uppercase tracking-widest">
                {device.model || 'Connected'}
              </span>
            </div>
          )}
        </div>
        <p className="text-gray-400">
          Advanced diagnostic and prerequisite tools for device lifecycle control.
        </p>
      </div>

      {!device && <ToolboxNoDeviceBanner />}

      {/* DFU Assistant */}
      <ToolCardGrid>
        <DfuAssistantCard />
      </ToolCardGrid>

      {/* Mode Entry & Exit */}
      <ToolboxSectionHeader title="Mode Entry & Exit" subtitle="Change device boot state" />
      <ToolCardGrid>
        <ToolCard
          title="Enter Recovery"
          description="Place device into Recovery Mode."
          operationType="RecoveryEnter"
          requiredMode={['normal']}
          riskLevel="low"
          requiresConfirm={false}
          disabled={!enterRecovery.enabled}
          disabledReason={enterRecovery.reason || undefined}
          icon={<Shield className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Exit Recovery"
          description="Kick device out of recovery mode loop."
          operationType="RecoveryExit"
          requiredMode="recovery"
          riskLevel="low"
          requiresConfirm={false}
          disabled={!exitRecovery.enabled}
          disabledReason={exitRecovery.reason || undefined}
          icon={<ShieldAlert className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Exit DFU"
          description="Attempt to exit DFU state (requires device restart)."
          operationType="DfuExit"
          requiredMode="dfu"
          riskLevel="low"
          requiresConfirm={false}
          disabled={!exitDfu.enabled}
          disabledReason={exitDfu.reason || undefined}
          icon={<Zap className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Purple Mode Entry"
          description="Load diagnostic ramdisk into memory to enable raw file system access."
          operationType="PurpleModeEntry"
          requiredMode="any"
          riskLevel="medium"
          requiresConfirm={false}
          disabled={!purpleMode.enabled}
          disabledReason={purpleMode.reason || undefined}
          icon={<Cpu className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Reboot to Hello"
          description="Reboot device and drop directly into the Hello setup screen."
          operationType="RebootToHello"
          requiredMode={['normal']}
          riskLevel="medium"
          requiresConfirm={false}
          disabled={!rebootHello.enabled}
          disabledReason={rebootHello.reason || undefined}
          icon={<Play className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Reboot"
          description="Standard device reboot."
          operationType="Reboot"
          requiredMode="any"
          riskLevel="low"
          requiresConfirm={false}
          disabled={!reboot.enabled}
          disabledReason={reboot.reason || undefined}
          icon={<Play className="w-5 h-5 text-gray-300" />}
        />
      </ToolCardGrid>

      {/* Boot Files */}
      <ToolboxSectionHeader title="Boot Files" subtitle="Interact with low-level boot artifacts" />
      <ToolCardGrid>
        <ToolCard
          title="Boot Files Activation"
          description="Patch and activate using custom boot files."
          operationType="BootFilesActivation"
          requiredMode="boot_files"
          riskLevel="high"
          requiresConfirm={true}
          confirmMessage="This will overwrite boot files on your device. Ensure you have a backup before proceeding."
          disabled={!bootFilesActivation.enabled}
          disabledReason={bootFilesActivation.reason || undefined}
          icon={<Key className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Boot Files Backup"
          description="Dump and backup current boot files for safety."
          operationType="BootFilesBackup"
          requiredMode="boot_files"
          riskLevel="high"
          requiresConfirm={true}
          confirmMessage="This will backup critical boot files. Continue?"
          disabled={!bootFilesBackup.enabled}
          disabledReason={bootFilesBackup.reason || undefined}
          icon={<Key className="w-5 h-5 text-gray-300" />}
        />
      </ToolCardGrid>

      {/* Protection */}
      <ToolboxSectionHeader title="Protection" subtitle="Device blocking operations" />
      <ToolCardGrid>
        <ToolCard
          title="OTA Block"
          description="Prevent over-the-air updates."
          operationType="OtaBlock"
          requiredMode={['normal']}
          riskLevel="low"
          requiresConfirm={true}
          confirmMessage="This will install an OTA blocker profile on the device."
          disabled={!otaBlock.enabled}
          disabledReason={otaBlock.reason || undefined}
          icon={<Shield className="w-5 h-5 text-gray-300" />}
        />
        <ToolCard
          title="Restore Block"
          description="Prevent iTunes restores."
          operationType="RestoreBlock"
          requiredMode={['normal']}
          riskLevel="low"
          requiresConfirm={true}
          confirmMessage="This will block iTunes restores. Proceed?"
          disabled={!restoreBlock.enabled}
          disabledReason={restoreBlock.reason || undefined}
          icon={<Shield className="w-5 h-5 text-gray-300" />}
        />
      </ToolCardGrid>

      {/* Diagnostics */}
      <ToolboxSectionHeader title="Diagnostics" />
      <DiagnosticsCard />

      {/* Jailbreak */}
      <ToolboxSectionHeader
        title="Jailbreak Launchers"
        subtitle="Requires compatible iOS and device"
      />
      {!jailbreakAccepted ? (
        <JailbreakDisclaimerGate onAccept={() => setJailbreakAccepted(true)} />
      ) : (
        <ToolCardGrid>
          <ToolCard
            title="palera1n"
            description="Launch palera1n jailbreak (iOS 15.0 - 16.7, A9-A11)."
            operationType="JailbreakPalera1n"
            requiredMode={['normal', 'recovery']}
            riskLevel="high"
            requiresConfirm={false}
            disabled={!palera1nCap.enabled || !palera1nCompat.compatible}
            disabledReason={
              !palera1nCompat.compatible ? palera1nCompat.reason : palera1nCap.reason || undefined
            }
            icon={<Zap className="w-5 h-5 text-gray-300" />}
          />
          <ToolCard
            title="checkra1n"
            description="Launch checkra1n jailbreak (iOS 12.0 - 14.8.1, A7-A11)."
            operationType="JailbreakCheckra1n"
            requiredMode={['normal', 'recovery']}
            riskLevel="high"
            requiresConfirm={false}
            disabled={!checkra1nCap.enabled || !checkra1nCompat.compatible}
            disabledReason={
              !checkra1nCompat.compatible
                ? checkra1nCompat.reason
                : checkra1nCap.reason || undefined
            }
            icon={<Zap className="w-5 h-5 text-gray-300" />}
          />
        </ToolCardGrid>
      )}
    </div>
  );
}
