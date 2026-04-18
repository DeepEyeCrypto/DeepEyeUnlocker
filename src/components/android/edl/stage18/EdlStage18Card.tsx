import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage18Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={18}
      title="Power Control"
      subtitle="Handles normal boot, EDL reboot, and reset transitions after Firehose operations complete."
      command="edl_stage18_power_control"
      serial={serial}
      accent="linear-gradient(90deg, rgba(8, 47, 73, 0.96), rgba(14, 165, 233, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
