import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage15Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={15}
      title="Partition Read"
      subtitle="Stages large controlled reads for imaging, forensics, and backup workflows."
      command="edl_stage15_partition_read"
      serial={serial}
      accent="linear-gradient(90deg, rgba(30, 41, 59, 0.96), rgba(71, 85, 105, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
