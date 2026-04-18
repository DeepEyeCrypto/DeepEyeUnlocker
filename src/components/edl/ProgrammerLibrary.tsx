import { useState, useEffect } from "react";
import { invoke } from "@tauri-apps/api/core";
import { Cpu, FileCode, Download, AlertTriangle } from "lucide-react";

interface ProgrammerEntry {
  device: string;
  chipset: string;
  programmer_name: string;
  sha256: string;
  notes: string;
}

export const ProgrammerLibrary = () => {
  const [programmers, setProgrammers] = useState<ProgrammerEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedProgrammer, setSelectedProgrammer] = useState<ProgrammerEntry | null>(null);

  useEffect(() => {
    loadProgrammers();
  }, []);

  const loadProgrammers = async () => {
    try {
      const list = await invoke("get_edl_programmers");
      setProgrammers(list as ProgrammerEntry[]);
    } catch (e) {
      console.error("Failed to load programmers:", e);
    } finally {
      setLoading(false);
    }
  };

  const handleLoadProgrammer = async (path: string) => {
    try {
      const result = await invoke("load_edl_programmer", { path });
      alert(result);
    } catch (e) {
      alert(`❌ Error: ${e}`);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-400">Loading programmer database...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2 bg-indigo-500/20 rounded-lg">
          <Cpu className="w-6 h-6 text-indigo-400" />
        </div>
        <div>
          <h2 className="text-xl font-bold text-white">EDL Programmer Library</h2>
          <p className="text-sm text-gray-400">{programmers.length} programmers available</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {programmers.map((prog, idx) => (
          <div
            key={idx}
            className={`p-4 bg-white/5 border rounded-2xl cursor-pointer transition-all hover:bg-white/10 ${
              selectedProgrammer === prog
                ? "border-indigo-500/50 bg-indigo-500/10"
                : "border-white/10"
            }`}
            onClick={() => setSelectedProgrammer(prog)}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1">
                <h3 className="text-white font-bold text-lg">{prog.device}</h3>
                <p className="text-sm text-gray-400">{prog.chipset}</p>
              </div>
              <FileCode className="w-5 h-5 text-indigo-400 flex-shrink-0 ml-2" />
            </div>

            <div className="bg-black/30 rounded-lg px-3 py-2 mb-3">
              <p className="text-xs text-gray-500 font-mono truncate">{prog.programmer_name}</p>
            </div>

            <div className="flex items-start gap-2 mb-3">
              <AlertTriangle className="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" />
              <p className="text-xs text-amber-300">{prog.notes}</p>
            </div>

            {prog.sha256 && (
              <div className="text-[10px] text-gray-600 font-mono">
                SHA256: {prog.sha256}
              </div>
            )}
          </div>
        ))}
      </div>

      {selectedProgrammer && (
        <div className="mt-6 p-4 bg-indigo-500/10 border border-indigo-500/30 rounded-2xl">
          <h3 className="text-white font-bold mb-2">Selected: {selectedProgrammer.device}</h3>
          <p className="text-sm text-gray-300 mb-4">
            Place the programmer file in <code className="bg-black/30 px-2 py-1 rounded">~/.deepeye/programmers/</code>
          </p>
          <button
            onClick={() => handleLoadProgrammer(`~/.deepeye/programmers/${selectedProgrammer.programmer_name}`)}
            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-2 px-4 rounded-xl transition-all"
          >
            <Download className="w-4 h-4" />
            Load Programmer
          </button>
        </div>
      )}

      <div className="mt-6 p-4 bg-amber-500/10 border border-amber-500/20 rounded-2xl">
        <p className="text-xs text-amber-300 leading-relaxed">
          <strong>⚠️ Important:</strong> EDL programmers are device-specific. Using the wrong programmer can brick your device.
          Always verify the chipset model and cross-reference with the device schematic before flashing.
        </p>
      </div>
    </div>
  );
};
