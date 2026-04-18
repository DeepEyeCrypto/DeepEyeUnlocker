import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage12Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={12}
      title="Userdata Format"
      subtitle="Stages the destructive erase sequence used when a full user partition reset is required."
      command="edl_stage12_userdata_format"
      serial={serial}
      accent="linear-gradient(90deg, rgba(14, 165, 233, 0.96), rgba(56, 189, 248, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
