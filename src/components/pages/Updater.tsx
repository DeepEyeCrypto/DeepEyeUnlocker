import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { UPDATER_COMMANDS, type UpdateInfo } from "../../lib/updater";
import { Card } from "../ui/Card";

export default function UpdaterPage() {
  const [status, setStatus] = useState<"idle" | "checking" | "available" | "installing" | "done" | "error">("idle");
  const [update, setUpdate] = useState<UpdateInfo | null>(null);
  const [message, setMessage] = useState("");

  const checkUpdate = async () => {
    setStatus("checking");
    setMessage("");
    try {
      const info = await invoke<UpdateInfo>(UPDATER_COMMANDS.CHECK);
      if (info.available) {
        setUpdate(info);
        setStatus("available");
      } else {
        setStatus("idle");
        setMessage("You are on the latest version.");
      }
    } catch (e: unknown) {
      setStatus("error");
      setMessage(String(e));
    }
  };

  const installUpdate = async () => {
    setStatus("installing");
    setMessage("");
    try {
      const result = await invoke<string>(UPDATER_COMMANDS.INSTALL);
      setMessage(result);
      setStatus("done");
    } catch (e: unknown) {
      setStatus("error");
      setMessage(String(e));
    }
  };

  return (
    <div className="page">
      <div className="row-between">
        <h2 className="page-title">Auto-Updater</h2>
        <button
          className="btn btn-primary btn-sm"
          disabled={status === "checking" || status === "installing"}
          onClick={checkUpdate}
        >
          {status === "checking" ? "Checking..." : "Check for Updates"}
        </button>
      </div>

      <Card title="Update Status">
        {status === "idle" && !message && (
          <p className="muted">Click "Check for Updates" to see if a new version is available.</p>
        )}

        {status === "idle" && message && (
          <div className="panel">
            <span className="action-title">{message}</span>
          </div>
        )}

        {status === "checking" && (
          <div className="panel pulse-panel">Checking for updates...</div>
        )}

        {status === "available" && update && (
          <div className="stack-sm">
            <div className="panel">
              <div className="device-grid">
                <div className="device-field">
                  <span className="device-field-label">New Version</span>
                  <span className="device-field-value highlight">v{update.version}</span>
                </div>
                <div className="device-field">
                  <span className="device-field-label">Release Date</span>
                  <span className="device-field-value">{update.date || "N/A"}</span>
                </div>
              </div>
              {update.body && (
                <div className="meta-text" style={{ marginTop: "var(--space-3)", whiteSpace: "pre-wrap" }}>
                  {update.body}
                </div>
              )}
            </div>
            <button className="btn btn-success btn-md" onClick={installUpdate}>
              Download &amp; Install
            </button>
          </div>
        )}

        {status === "installing" && (
          <div className="panel pulse-panel">
            <div className="action-title">Downloading and installing update...</div>
            <p className="meta-text">Do not close the application.</p>
          </div>
        )}

        {status === "done" && (
          <div className="panel">
            <div className="action-title">{message}</div>
          </div>
        )}

        {status === "error" && (
          <div className="danger-note">
            <div className="danger-title">Update Error</div>
            <span className="meta-text">{message}</span>
          </div>
        )}
      </Card>
    </div>
  );
}
