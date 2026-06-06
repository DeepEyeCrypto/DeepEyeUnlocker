import { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { EDL_COMMANDS, type EdlDeviceInfo } from '../lib/edl';
import { useAppStore } from '../stores/useAppStore';
import { Cpu, Search, HardDrive, TerminalSquare, AlertTriangle, RefreshCw } from 'lucide-react';
import { ToolboxActionCard } from '../components/ui/ToolboxActionCard';

export function EdlScreen() {
  const { appendLog, startOperation, endOperation, activeOperation } = useAppStore();
  const [device, setDevice] = useState<EdlDeviceInfo | null>(null);
  const [partition, setPartition] = useState('boot');
  const [filePath, setFilePath] = useState('');
  const [gptData, setGptData] = useState('');

  const isDetecting = activeOperation === 'edl_detect';
  const isRunning = activeOperation !== null && activeOperation !== 'edl_detect';
  const connected = device !== null;

  const detect = async () => {
    startOperation('edl_detect');
    appendLog('info', 'Scanning for Qualcomm EDL 9008 device...');
    try {
      const info = await invoke<EdlDeviceInfo>(EDL_COMMANDS.DETECT);
      if (info.detected) {
        setDevice(info);
        appendLog('success', `EDL Device detected: ${info.chipset} (${info.serial || 'N/A'})`);
      } else {
        setDevice(null);
        appendLog('warn', 'No EDL 9008 device found on USB bus.');
      }
    } catch (e) {
      setDevice(null);
      appendLog('error', `Detection failed: ${String(e)}`);
    } finally {
      endOperation();
    }
  };

  const runCmd = async (cmd: string, args: Record<string, string> = {}) => {
    startOperation(cmd);
    appendLog('info', `Executing EDL Command: ${cmd}`);
    try {
      const result = await invoke<string>(cmd, args);
      appendLog('success', `Success: ${result}`);
      if (cmd === EDL_COMMANDS.GET_GPT) {
        setGptData(result);
      }
    } catch (e) {
      appendLog('error', `Failed: ${String(e)}`);
    } finally {
      endOperation();
    }
  };

  return (
    <div className="flex flex-col gap-8 animate-in fade-in slide-in-from-bottom-4 duration-500 pb-12">
      <div className="flex flex-col gap-2">
        <h2 className="text-3xl font-bold text-white tracking-tight">Qualcomm EDL</h2>
        <p className="text-gray-400">
          Emergency Download Mode (9008) partition management and flash tools.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Hardware Status */}
        <div className="flex flex-col gap-4 p-6 rounded-2xl backdrop-blur-xl bg-white/5 border border-white/10 shadow-[0_32px_64px_rgba(0,0,0,0.4)]">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Cpu className="w-5 h-5 text-purple-400" /> Target Hardware
            </h3>
            <button
              className="px-4 py-2 bg-[#7C3AED] hover:bg-[#6D28D9] disabled:bg-gray-600 disabled:cursor-not-allowed text-white text-sm font-bold rounded-lg transition-colors flex items-center gap-2 shadow-lg"
              onClick={detect}
              disabled={isDetecting || isRunning}
            >
              {isDetecting ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <Search className="w-4 h-4" />
              )}
              {isDetecting ? 'Scanning...' : 'Detect 9008'}
            </button>
          </div>

          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between p-3 bg-black/40 rounded-xl border border-white/5">
              <span className="text-sm text-gray-400">Status</span>
              <span
                className={`text-sm font-bold ${connected ? 'text-green-400' : 'text-red-400'}`}
              >
                {connected ? 'CONNECTED' : 'DISCONNECTED'}
              </span>
            </div>

            <div className="flex items-center justify-between p-3 bg-black/40 rounded-xl border border-white/5">
              <span className="text-sm text-gray-400">Chipset ID</span>
              <span className="text-sm font-bold text-white font-mono">
                {device?.chipset || 'WAITING'}
              </span>
            </div>

            <div className="flex items-center justify-between p-3 bg-black/40 rounded-xl border border-white/5">
              <span className="text-sm text-gray-400">Serial No.</span>
              <span className="text-sm font-bold text-white font-mono">
                {device?.serial || 'WAITING'}
              </span>
            </div>
          </div>
        </div>

        {/* Partition Target */}
        <div className="flex flex-col gap-4 p-6 rounded-2xl backdrop-blur-xl bg-white/5 border border-white/10 shadow-[0_32px_64px_rgba(0,0,0,0.4)]">
          <h3 className="text-lg font-bold text-white flex items-center gap-2 mb-2">
            <HardDrive className="w-5 h-5 text-cyan-400" /> Partition Target
          </h3>

          <div className="flex flex-col gap-2">
            <label className="text-sm text-gray-400">Partition Name</label>
            <select
              value={partition}
              onChange={(e) => setPartition(e.target.value)}
              className="bg-black/50 border border-white/10 rounded-lg px-4 py-2 text-white outline-none focus:border-[#7C3AED] transition-colors appearance-none"
            >
              {[
                'boot',
                'recovery',
                'system',
                'userdata',
                'modem',
                'fsg',
                'persist',
                'misc',
                'aboot',
                'sbl1',
                'tz',
                'rpm',
              ].map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-2 mt-2">
            <label className="text-sm text-gray-400">Local Image Path</label>
            <input
              type="text"
              value={filePath}
              onChange={(e) => setFilePath(e.target.value)}
              placeholder={`/tmp/${partition}.img`}
              className="bg-black/50 border border-white/10 rounded-lg px-4 py-2 text-white font-mono text-sm outline-none focus:border-[#7C3AED] transition-colors"
            />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-2">
        <ToolboxActionCard
          title="Read Partition"
          description="Dump target partition to the specified local image path."
          icon={<TerminalSquare className="w-6 h-6 text-white" />}
          tag="SAFE"
          disabled={!connected || isRunning}
          onClick={() =>
            runCmd(EDL_COMMANDS.READ_PARTITION, {
              partition,
              output_path: filePath || `/tmp/${partition}.img`,
            })
          }
        />

        <ToolboxActionCard
          title="Write Image"
          description="Flash a raw image file directly to the target partition."
          icon={<Cpu className="w-6 h-6 text-white" />}
          tag="FLASH"
          disabled={!connected || isRunning || !filePath}
          onClick={() => runCmd(EDL_COMMANDS.WRITE_PARTITION, { partition, image_path: filePath })}
        />

        <ToolboxActionCard
          title="Erase Partition"
          description="Wipe all data blocks on the target partition (Destructive)."
          icon={<AlertTriangle className="w-6 h-6 text-red-400" />}
          tag="DANGER"
          disabled={!connected || isRunning}
          onClick={() => runCmd(EDL_COMMANDS.ERASE_PARTITION, { partition })}
        />

        <ToolboxActionCard
          title="Get GPT / Reboot"
          description="Print raw partition table or exit EDL mode safely."
          icon={<HardDrive className="w-6 h-6 text-cyan-400" />}
          tag="UTILS"
          disabled={!connected || isRunning}
          onClick={() => {
            if (gptData) runCmd(EDL_COMMANDS.REBOOT);
            else runCmd(EDL_COMMANDS.GET_GPT);
          }}
        />
      </div>

      {gptData && (
        <div className="p-4 bg-black/60 rounded-xl border border-cyan-500/30 max-h-64 overflow-y-auto mt-4 animate-in fade-in">
          <pre className="text-xs text-cyan-300 whitespace-pre-wrap font-mono">{gptData}</pre>
        </div>
      )}
    </div>
  );
}
