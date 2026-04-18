import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage16Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={16}
      title="Partition Write"
      subtitle="Stages raw image programming once the correct label, size, and loader are confirmed."
      command="edl_stage16_partition_write"
      serial={serial}
      accent="linear-gradient(90deg, rgba(37, 99, 235, 0.96), rgba(96, 165, 250, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
