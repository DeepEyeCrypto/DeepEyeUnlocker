export interface QcomDevice {
  vid: string
  pid: string
  port: string
  device_name: string
  is_edl_mode: boolean
  is_fastboot: boolean
  is_adb: boolean
  chipset_hint: string
  serial: string
  brand_hint: string
}

export interface EdlStage1Result {
  devices: QcomDevice[]
  selected_serial: string
  edl_count: number
  fastboot_count: number
  adb_count: number
  qdl_available: boolean
  edl_tool_available: boolean
  adb_available: boolean
  fastboot_available: boolean
  edl_prog_found: boolean
  firehose_path: string
  how_to_edl: string[]
  stage_passed: boolean
  stage_message: string
}

export interface EdlPipelineStageResult {
  stage: number
  title: string
  subtitle: string
  next_stage_title: string
  serial: string
  stage_passed: boolean
  stage_message: string
  tool_name: string
  tool_available: boolean
  firehose_path: string
  firehose_found: boolean
  suggested_actions: string[]
}

export interface EdlStageCardProps {
  serial: string
  onPass?: (result: EdlPipelineStageResult) => void
  onBack?: () => void
  onClose?: () => void
}
