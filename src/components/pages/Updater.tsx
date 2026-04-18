import {
  summarizeChangelog,
  type UpdateInfo,
  type UpdateStatus,
} from "../../lib/updater";
import { Card } from "../ui/Card";
import { getVersion } from "@tauri-apps/api/app";
import { useEffect, useState } from "react";

type UpdaterPageProps = {
  status: UpdateStatus;
  update: UpdateInfo | null;
  message: string;
  onCheck: () => Promise<void>;
  onInstall: () => Promise<void>;
};

export default function UpdaterPage({
  status,
  update,
  message,
  onCheck,
  onInstall,
}: UpdaterPageProps) {
  const [currentVersion, setCurrentVersion] = useState("unknown");
  
  useEffect(() => {
    getVersion().then(setCurrentVersion).catch(() => setCurrentVersion("2027.18.1"));
  }, []);
  
  const summary = update ? summarizeChangelog(update.body, 4) : "";

  return (
    <div className="page">
      <div className="row-between">
        <h2 className="page-title">Auto-Updater</h2>
        <button
          className="btn btn-primary btn-sm"
          disabled={status === "checking" || status === "installing"}
          onClick={() => void onCheck()}
        >
          {status === "checking" ? "Checking..." : "Check for Updates"}
        </button>
      </div>

      <Card title="Update Status">
        {status === "idle" && !message && (
          <p className="muted">Click "Check for Updates" to see if a new version is available.</p>
        )}

        {status === "upToDate" && message && (
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
                  <span className="device-field-label">Current Version</span>
                  <span className="device-field-value">v{currentVersion}</span>
                </div>
                <div className="device-field">
                  <span className="device-field-label">Latest Version</span>
                  <span className="device-field-value highlight">v{update.version}</span>
                </div>
                <div className="device-field">
                  <span className="device-field-label">Release Date</span>
                  <span className="device-field-value">{update.date || "N/A"}</span>
                </div>
              </div>
              {summary && (
                <div className="meta-text" style={{ marginTop: "var(--space-3)", whiteSpace: "pre-wrap" }}>
                  {summary}
                </div>
              )}
              <div className="meta-text" style={{ marginTop: "var(--space-3)", wordBreak: "break-all" }}>
                {update.downloadUrl}
              </div>
            </div>
            <button className="btn btn-success btn-md" onClick={() => void onInstall()}>
              Install &amp; Restart
            </button>
          </div>
        )}

        {status === "installing" && (
          <div className="panel pulse-panel">
            <div className="action-title">Downloading and installing update...</div>
            <p className="meta-text">Do not close the application.</p>
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
