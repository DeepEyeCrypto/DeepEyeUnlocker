import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage20Card({ serial, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={20}
      title="Completion"
      subtitle="Finalizes the expanded 20-stage Qualcomm EDL pipeline and preserves the working session summary."
      command="edl_stage20_complete"
      serial={serial}
      accent="linear-gradient(90deg, rgba(20, 83, 45, 0.96), rgba(14, 165, 233, 0.92))"
      onBack={onBack}
      onClose={onClose}
      isFinalStage
    />
  )
}
