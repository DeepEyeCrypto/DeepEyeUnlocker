import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage4Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={4}
      title="Firehose Upload"
      subtitle="Stages the Sahara READ_DATA flow used to stream the selected programmer into RAM."
      command="edl_stage4_firehose_upload"
      serial={serial}
      accent="linear-gradient(90deg, rgba(67, 56, 202, 0.96), rgba(99, 102, 241, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
