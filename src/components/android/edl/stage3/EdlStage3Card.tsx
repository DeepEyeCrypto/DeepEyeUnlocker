import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage3Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={3}
      title="Programmer Selection"
      subtitle="Resolves the correct Firehose loader so later stages use the right chipset and storage profile."
      command="edl_stage3_programmer"
      serial={serial}
      accent="linear-gradient(90deg, rgba(88, 28, 135, 0.96), rgba(168, 85, 247, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
