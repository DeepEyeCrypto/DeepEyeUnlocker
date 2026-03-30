import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import Terminal from "../Terminal";

export default function FmiPage() {
  const [output, setOutput] = useState("");
  const [status, setStatus] = useState<"idle"|"running"|"success"|"error">("idle");

  const run = async (id: string, args: Record<string, any> = {}) => {
    setStatus("running"); setOutput("");
    try {
      const res = await invoke<string>(id, args);
      setOutput(res);
      setStatus("success");
    } catch (e: any) {
      setOutput(String(e));
      setStatus("error");
    }
  };

  return (
    <div className="page">
      <h2 className="page-title">FMI / iCloud</h2>
      <p className="page-subtitle">
        Find My iPhone Status · Apple ID Removal · Token Cleaning
      </p>

      <div className="grid-two">
        <div className="panel stack-sm">
          <div className="action-title">FMI Intel</div>
          <button className="btn btn-primary btn-sm" onClick={() => run("ios_fmi_state")}>Read FMI Status</button>
          <div className="meta-text">Checks if Find My is enabled via official Apple servers.</div>
        </div>

        <div className="panel stack-sm">
          <div className="action-title">Account Logic</div>
          <button className="btn btn-secondary btn-sm" onClick={() => run("ios_apple_id_state")}>Check Apple ID</button>
          <button className="btn btn-danger btn-sm" onClick={() => run("ios_remove_apple_id")}>Remove Apple ID</button>
        </div>
      </div>

      <div className="danger-note">
        <div className="danger-title">PRO FORENSIC MODE</div>
        <p className="meta-text">
          FMI OFF requires valid session tokens or an open menu state. If the menu is closed, use the <strong>MASS EXTRACTION</strong> tool to attempt token recovery before running removal logic.
        </p>
      </div>

      <Terminal output={output} status={status} />
    </div>
  );
}
