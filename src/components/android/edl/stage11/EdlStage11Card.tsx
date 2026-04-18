import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage11Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={11}
      title="Userdata Plan"
      subtitle="Calculates safe partition-level targeting before a userdata wipe or extraction run begins."
      command="edl_stage11_userdata_plan"
      serial={serial}
      accent="linear-gradient(90deg, rgba(91, 33, 182, 0.96), rgba(139, 92, 246, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
