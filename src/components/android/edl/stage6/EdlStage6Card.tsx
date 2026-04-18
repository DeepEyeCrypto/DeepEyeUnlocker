import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage6Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={6}
      title="Storage Probe"
      subtitle="Checks storage geometry before partition operations start, including payload and sector constraints."
      command="edl_stage6_storage_probe"
      serial={serial}
      accent="linear-gradient(90deg, rgba(13, 148, 136, 0.96), rgba(45, 212, 191, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
