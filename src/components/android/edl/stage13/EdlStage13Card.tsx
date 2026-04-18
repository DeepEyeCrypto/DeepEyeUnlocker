import { EdlStageTemplateCard } from "../EdlStageTemplateCard"
import type { EdlStageCardProps } from "../types"

export function EdlStage13Card({ serial, onPass, onBack, onClose }: EdlStageCardProps) {
  return (
    <EdlStageTemplateCard
      stage={13}
      title="Persist Backup"
      subtitle="Protects calibration and device-state partitions before any later writeback experiments."
      command="edl_stage13_persist_backup"
      serial={serial}
      accent="linear-gradient(90deg, rgba(6, 95, 70, 0.96), rgba(20, 184, 166, 0.92))"
      onPass={onPass}
      onBack={onBack}
      onClose={onClose}
    />
  )
}
