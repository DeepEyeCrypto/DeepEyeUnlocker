import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage19Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={19}
      title="Verification"
      subtitle="Re-checks tool readiness, programmer availability, and session continuity before final closeout."
      command="edl_stage19_verify"
      serial={serial}
      accent="linear-gradient(90deg, rgba(22, 163, 74, 0.96), rgba(74, 222, 128, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
