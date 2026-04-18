import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage5Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={5}
      title="Firehose Configure"
      subtitle="Prepares the XML configure packet that unlocks stable read, write, and erase transactions."
      command="edl_stage5_firehose_config"
      serial={serial}
      accent="linear-gradient(90deg, rgba(30, 64, 175, 0.96), rgba(59, 130, 246, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
