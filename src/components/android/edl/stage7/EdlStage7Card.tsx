import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage7Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={7}
      title="GPT Read"
      subtitle="Reads the partition table stage so the pipeline can target labels instead of guessed offsets."
      command="edl_stage7_gpt"
      serial={serial}
      accent="linear-gradient(90deg, rgba(22, 101, 52, 0.96), rgba(34, 197, 94, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
