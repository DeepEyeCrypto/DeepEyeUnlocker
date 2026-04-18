import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage17Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={17}
      title="XML Console"
      subtitle="Surfaces raw Firehose XML planning for advanced configure, read, erase, and power commands."
      command="edl_stage17_xml_console"
      serial={serial}
      accent="linear-gradient(90deg, rgba(88, 28, 135, 0.96), rgba(192, 132, 252, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
