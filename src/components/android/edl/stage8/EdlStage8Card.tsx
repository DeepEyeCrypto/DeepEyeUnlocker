import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage8Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={8}
      title="Partition Map"
      subtitle="Converts GPT output into an actionable map for FRP, userdata, modem, persist, and boot slots."
      command="edl_stage8_partition_map"
      serial={serial}
      accent="linear-gradient(90deg, rgba(120, 53, 15, 0.96), rgba(249, 115, 22, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
