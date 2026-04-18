import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage9Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={9}
      title="FRP Plan"
      subtitle="Builds the partition-scoped plan used for controlled FRP removal workflows."
      command="edl_stage9_frp_plan"
      serial={serial}
      accent="linear-gradient(90deg, rgba(190, 24, 93, 0.96), rgba(244, 63, 94, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
