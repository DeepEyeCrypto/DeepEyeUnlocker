import { useMemo, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";
import { Card } from "../ui/Card";

type FrpMethod = "adb_sideload" | "odin_flash";

const FRP_RISK: Record<FrpMethod, { summary: string; partitions: string }> = {
  // [INFERRED] Samsung FRP bypass payloads typically touch FRP-related reset paths and can trigger userdata/cache wipes.
  adb_sideload: {
    summary: "ADB recovery sideload path for Samsung FRP remediation.",
    partitions: "userdata, cache, FRP-related Samsung recovery payload targets",
  },
  // [INFERRED] ODIN FRP packages typically write vendor-selected FRP/reset targets and may wipe userdata.
  odin_flash: {
    summary: "ODIN flash path for Samsung FRP remediation.",
    partitions: "userdata, cache, vendor FRP/reset package targets",
  },
};

export default function FrpBypassPage() {
  const [status, setStatus] = useState<"idle" | "running" | "success" | "error">("idle");
  const [output, setOutput] = useState("");
  const [acknowledged, setAcknowledged] = useState(false);

  const methods = useMemo(
    () => [
      { id: "adb_sideload" as const, title: "Samsung Recovery / ADB Sideload" },
      { id: "odin_flash" as const, title: "Samsung ODIN Flash" },
    ],
    [],
  );

  const runBypass = async (method: FrpMethod) => {
    const risk = FRP_RISK[method];
    const confirmed = window.confirm(
      `Operation: Samsung FRP bypass\nRisk: HIGH\nAffected partitions/targets: ${risk.partitions}\n\nThis operation may permanently change reset state on the connected device. Continue?`,
    );

    if (!confirmed) {
      return;
    }

    setStatus("running");
    setOutput("");

    try {
      const result = await invoke<string>("hydra_samsung_frp_bypass", { method });
      setOutput(result);
      setStatus("success");
    } catch (error: unknown) {
      setOutput(String(error));
      setStatus("error");
    }
  };

  return (
    <div className="page">
      <div>
        <h2 className="page-title">FRP Bypass</h2>
        <p className="page-subtitle">Android-focused Samsung FRP operations with explicit desktop-vs-mobile visibility</p>
      </div>

      <div className="danger-note">
        HIGH RISK — FRP operations can permanently change reset state and may wipe user data depending on the selected vendor workflow.
      </div>

      <Card title="Safety Gate">
        <div className="stack-sm">
          <div className="panel">Recommend backing up EFS before proceeding when a vendor workflow may touch modem, persist, or FRP-related reset targets.</div>
          <label className="field-label" style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} />
            I understand Samsung FRP workflows may affect userdata, cache, and FRP-related partitions.
          </label>
        </div>
      </Card>

      <Card title="Samsung FRP Methods">
        <div className="stack-sm">
          {methods.map((method) => (
            <div key={method.id} className="panel">
              <div className="row-between" style={{ gap: 16, alignItems: "center" }}>
                <div>
                  <div className="action-title">{method.title}</div>
                  <div className="page-subtitle">{FRP_RISK[method.id].summary}</div>
                </div>
                <button
                  className="btn btn-danger btn-sm"
                  disabled={!acknowledged || status === "running"}
                  onClick={() => void runBypass(method.id)}
                  title={!acknowledged ? "Acknowledge the safety gate first" : method.title}
                >
                  Run Method
                </button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Terminal output={output} status={status} />
    </div>
  );
}
