import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage14Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={14}
      title="Modem/EFS Backup"
      subtitle="Captures radio-critical partitions like modemst and fsg before restoration or repair tasks."
      command="edl_stage14_modem_backup"
      serial={serial}
      accent="linear-gradient(90deg, rgba(146, 64, 14, 0.96), rgba(251, 146, 60, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
