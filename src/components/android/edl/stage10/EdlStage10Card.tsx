import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage10Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={10}
      title="FRP Erase"
      subtitle="Queues the validated erase operation after the dedicated FRP label has been mapped."
      command="edl_stage10_frp_erase"
      serial={serial}
      accent="linear-gradient(90deg, rgba(153, 27, 27, 0.96), rgba(239, 68, 68, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
