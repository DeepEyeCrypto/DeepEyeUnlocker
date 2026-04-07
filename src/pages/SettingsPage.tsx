import { invoke } from "@tauri-apps/api/core";
import { type as osType } from "@tauri-apps/plugin-os";
import { useEffect, useState, type ReactNode } from "react";
import { LiquidMetalButton } from "../components/ui/liquid-metal-button";
import type { AppSettings, DetectInterval } from "../lib/settings";
import type { UpdateInfo, UpdateStatus } from "../lib/updater";
import "../styles/settings.css";

type SettingsPageProps = {
  settings: AppSettings;
  onSettingsChange: (next: Partial<AppSettings>) => void;
  onCheckForUpdates: () => Promise<void>;
  updateStatus: UpdateStatus;
  updateInfo: UpdateInfo | null;
  updateMessage: string;
};

function clampTcpPort(value: number): number {
  return Math.min(65535, Math.max(1, Math.trunc(value)));
}

export default function SettingsPage({
  settings,
  onSettingsChange,
  onCheckForUpdates,
  updateStatus,
  updateInfo,
  updateMessage,
}: SettingsPageProps) {
  const [platformType, setPlatformType] = useState("unknown");
  const [adbTestOutput, setAdbTestOutput] = useState("");
  const [adbTesting, setAdbTesting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadPlatform = async () => {
      try {
        const currentType = await osType();
        if (!cancelled) {
          setPlatformType(currentType);
        }
      } catch {
        if (!cancelled) {
          setPlatformType("unknown");
        }
      }
    };

    void loadPlatform();

    return () => {
      cancelled = true;
    };
  }, []);

  const testAdb = async () => {
    setAdbTesting(true);
    setAdbTestOutput("");

    try {
      const result = await invoke<string>("adb_test_binary", {
        path: settings.adbBinaryPath.trim() || "adb",
      });
      setAdbTestOutput(result);
    } catch (error) {
      setAdbTestOutput(String(error));
    } finally {
      setAdbTesting(false);
    }
  };

  return (
    <div className="settings-page">
      <section className="settings-section">
        <div className="settings-section__header">
          <div>
            <p className="settings-section__eyebrow">App Info</p>
            <h2 className="settings-section__title">DeepEyeUnlocker</h2>
          </div>

          <LiquidMetalButton
            label={updateStatus === "checking" ? "Checking..." : "Check for Updates"}
            onClick={onCheckForUpdates}
            disabled={updateStatus === "checking" || updateStatus === "installing"}
          />
        </div>

        <div className="settings-grid">
          <InfoRow label="App name" value="DeepEyeUnlocker" />
          <InfoRow label="Version" value={__APP_VERSION__} />
          <InfoRow label="Build" value={__CARGO_PKG_VERSION__} />
          <InfoRow label="Platform" value={platformType} />
        </div>

        {(updateMessage || updateInfo) && (
          <div className="settings-note">
            {updateInfo ? `Update v${updateInfo.version} is ready to install.` : updateMessage}
          </div>
        )}
      </section>

      <section className="settings-section">
        <div className="settings-section__header">
          <div>
            <p className="settings-section__eyebrow">ADB Config</p>
            <h2 className="settings-section__title">Desktop bridge configuration</h2>
          </div>
        </div>

        <div className="settings-form-grid">
          <label className="settings-field settings-field--wide">
            <span className="settings-field__label">ADB binary path</span>
            <input
              className="settings-input"
              value={settings.adbBinaryPath}
              onChange={(event) =>
                onSettingsChange({ adbBinaryPath: event.target.value || "adb" })
              }
            />
          </label>

          <div className="settings-field settings-field--wide settings-field__actions">
            <button
              type="button"
              className="settings-button settings-button--secondary"
              onClick={() => void testAdb()}
              disabled={adbTesting}
            >
              {adbTesting ? "Testing..." : "Test ADB"}
            </button>
          </div>

          <label className="settings-toggle">
            <span>
              <span className="settings-field__label">ADB over TCP</span>
              <span className="settings-field__help">Enable TCP-based device bridging</span>
            </span>
            <input
              type="checkbox"
              checked={settings.adbOverTcp}
              onChange={(event) =>
                onSettingsChange({ adbOverTcp: event.target.checked })
              }
            />
          </label>

          <label className="settings-field">
            <span className="settings-field__label">TCP port</span>
            <input
              className="settings-input"
              type="number"
              min={1}
              max={65535}
              value={settings.tcpPort}
              onChange={(event) =>
                onSettingsChange({
                  tcpPort: clampTcpPort(Number(event.target.value || settings.tcpPort)),
                })
              }
            />
          </label>
        </div>

        {adbTestOutput && <pre className="settings-terminal">{adbTestOutput}</pre>}
      </section>

      <section className="settings-section">
        <div className="settings-section__header">
          <div>
            <p className="settings-section__eyebrow">USB Config</p>
            <h2 className="settings-section__title">Detection and logging</h2>
          </div>
        </div>

        <div className="settings-form-grid">
          <label className="settings-field">
            <span className="settings-field__label">Auto-detect interval</span>
            <select
              className="settings-select"
              value={settings.usbDetectIntervalMs}
              onChange={(event) =>
                onSettingsChange({
                  usbDetectIntervalMs: Number(event.target.value) as DetectInterval,
                })
              }
            >
              <option value={1000}>1s</option>
              <option value={2000}>2s</option>
              <option value={5000}>5s</option>
            </select>
          </label>

          <label className="settings-toggle">
            <span>
              <span className="settings-field__label">USB debug logging</span>
              <span className="settings-field__help">Append verbose polling traces to the console</span>
            </span>
            <input
              type="checkbox"
              checked={settings.usbDebugLogging}
              onChange={(event) =>
                onSettingsChange({ usbDebugLogging: event.target.checked })
              }
            />
          </label>
        </div>
      </section>

      <section className="settings-section">
        <div className="settings-section__header">
          <div>
            <p className="settings-section__eyebrow">About</p>
            <h2 className="settings-section__title">Project metadata</h2>
          </div>
        </div>

        <div className="settings-grid">
          <InfoRow
            label="GitHub"
            value={
              <a
                className="settings-link"
                href="https://github.com/DeepEyeCrypto/DeepEyeUnlocker"
                target="_blank"
                rel="noreferrer"
              >
                github.com/DeepEyeCrypto/DeepEyeUnlocker
              </a>
            }
          />
          <InfoRow label="License" value="MIT" />
          <InfoRow label="Author" value="DeepEye Team" />
        </div>
      </section>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="settings-info-row">
      <span className="settings-info-row__label">{label}</span>
      <span className="settings-info-row__value">{value}</span>
    </div>
  );
}
