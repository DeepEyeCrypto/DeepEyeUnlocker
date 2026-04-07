import { summarizeChangelog, type UpdateInfo } from "../lib/updater";
import { LiquidMetalButton } from "./ui/liquid-metal-button";

type UpdateBannerProps = {
  update: UpdateInfo;
  installing: boolean;
  onInstall: () => Promise<void> | void;
  onDismiss: () => void;
};

export function UpdateBanner({
  update,
  installing,
  onInstall,
  onDismiss,
}: UpdateBannerProps) {
  const summary = summarizeChangelog(update.body);

  return (
    <div className="update-banner" role="status" aria-live="polite">
      <div className="update-banner__content">
        <span className="update-banner__label">🆕 v{update.version} available</span>
        {summary && <span className="update-banner__summary">{summary}</span>}
      </div>

      <div className="update-banner__actions">
        <LiquidMetalButton
          label={installing ? "Installing..." : "Install & Restart"}
          onClick={onInstall}
          disabled={installing}
        />

        <button
          type="button"
          className="update-banner__dismiss"
          onClick={onDismiss}
          aria-label="Dismiss update notification"
        >
          ✕
        </button>
      </div>
    </div>
  );
}
