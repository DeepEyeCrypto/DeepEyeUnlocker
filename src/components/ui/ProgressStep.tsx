import { isMobile } from "../../lib/platform";

export type ProgressStatus = "idle" | "active" | "done" | "error";

export type ProgressItem = {
  id: string;
  label: string;
  status: ProgressStatus;
};

export function ProgressStep({ steps }: { steps: ProgressItem[] }) {
  const mobile = isMobile();
  return (
    <div className={`progress-step ${mobile ? "vertical" : "horizontal"}`}>
      {steps.map((step) => (
        <div key={step.id} className={`progress-node ${step.status}`}>
          <div className="progress-dot" />
          <span className="progress-label">{step.label}</span>
        </div>
      ))}
    </div>
  );
}

