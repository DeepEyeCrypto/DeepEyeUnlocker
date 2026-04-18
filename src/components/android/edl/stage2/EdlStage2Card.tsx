import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage2Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={2}
      title="Sahara Handshake"
      subtitle="Validates the first protocol exchange after Stage 1 device detection locks onto Qualcomm 9008 transport."
      command="edl_stage2_sahara"
      serial={serial}
      accent="linear-gradient(90deg, rgba(14, 116, 144, 0.96), rgba(6, 182, 212, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
