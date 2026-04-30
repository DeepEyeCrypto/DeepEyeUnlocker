import { useState, useCallback } from "react"
import { invoke } from "@tauri-apps/api/core"
import { Stage1Card }  from "./stage1/Stage1Card"
import { Stage2Card }  from "./stage2/Stage2Card"
import { Stage3Card }  from "./stage3/Stage3Card"
import { Stage4Card }  from "./stage4/Stage4Card"
import { Stage5Card }  from "./stage5/Stage5Card"
import { Stage6Card }  from "./stage6/Stage6Card"
import { Stage7Card }  from "./stage7/Stage7Card"
import { Stage8Card }  from "./stage8/Stage8Card"
import { Stage9Card }  from "./stage9/Stage9Card"
import { Stage10Card } from "./stage10/Stage10Card"

import "./SignalBypassFlow.css"

// Stage definitions for auto-run
const STAGES = [
  { id: 1, name: "Device Scan & Detection", tauriCommand: "signal_stage1_detect" },
  { id: 2, name: "Activation State Check", tauriCommand: "signal_stage2_activation" },
  { id: 3, name: "Baseband Analysis",       tauriCommand: "signal_stage3_baseband" },
  { id: 4, name: "iCloud Deep Scan",        tauriCommand: "signal_stage4_icloud" },
  { id: 5, name: "MDM Profile Removal",     tauriCommand: "signal_stage5_mdm" },
  { id: 6, name: "Carrier Registration",    tauriCommand: "signal_stage6_carrier" },
  { id: 7, name: "IMEI Re-registration",    tauriCommand: "signal_stage7_imei" },
  { id: 8, name: "Baseband Patch",          tauriCommand: "signal_stage8_baseband" },
  { id: 9, name: "Signal Bypass Score",     tauriCommand: "signal_stage9_verify" },
  { id: 10, name: "Final Verification",     tauriCommand: "signal_stage10_complete" },
]

export function SignalBypassFlow({
  onClose,
}: {
  onClose: () => void
}) {
  const [stage, setStage]       = useState(1)
  const [udid, setUdid]         = useState("")
  const [s9Score, setS9Score]   = useState(0)
  const [isRunningAll, setIsRunningAll] = useState(false)
  const [autoMode, setAutoMode] = useState(false)
  const [currentStage, setCurrentStage] = useState(1)
  const [logs, setLogs] = useState<string[]>([])
  const [finalResult, setFinalResult] = useState(false)
  const [stageResults, setStageResults] = useState<Record<number, any>>({})

  // Auto-run all 10 stages engine
  const runAllStagesAuto = useCallback(async () => {
    setIsRunningAll(true)
    setAutoMode(true)
    setFinalResult(false)
    setLogs([])
    setStageResults({})

    const addLog = (msg: string) => {
      setLogs(prev => [...prev, msg])
      console.log(msg)
    }

    addLog("🚀 Starting FULLY AUTOMATED BYPASS — 10 Stages")
    addLog("═══════════════════════════════════════")

    for (let i = 0; i < STAGES.length; i++) {
      const stageDef = STAGES[i]
      const stageNum = i + 1

      // Update UI: highlight current stage and transition cards
      setCurrentStage(stageNum)
      setStage(stageNum) 
      addLog(`\n⚡ Running Stage ${stageNum}/10: ${stageDef.name}...`)

      try {
        const currentUdid = udid || (stageResults[1] as any)?.udid;
        const result = await invoke(
          stageDef.tauriCommand,
          { udid: currentUdid, stageIndex: i }
        )

        setStageResults(prev => ({ ...prev, [stageNum]: result }))
        addLog(`✅ Stage ${stageNum} success: ${(result as any).stage_message || 'OK'}`)

        if (stageNum === 1 && result && (result as any).udid) {
          setUdid((result as any).udid)
        }

        if (stageNum === 9 && result && (result as any).bypass_score) {
          setS9Score((result as any).bypass_score)
        }

        await new Promise(r => setTimeout(r, 1200))

      } catch (err) {
        addLog(`❌ Stage ${stageNum} failed: ${err}`)
        addLog(`⚠️ Attempting to bypass failure and continue...`)
        await new Promise(r => setTimeout(r, 1000))
      }
    }

    setIsRunningAll(false)
    setAutoMode(false)
    setFinalResult(true)
    setStage(10) 
    addLog("\n═══════════════════════════════════════")
    addLog("🎯 AUTOMATION COMPLETE — Pipeline Finished")
    addLog("═══════════════════════════════════════")
  }, [udid, stageResults])

  function renderStage() {
    switch (stage) {
      case 1:
        return <Stage1Card
          onPass={(r) => {
            setUdid(r.udid)
            setStage(2)
          }}
          onClose={onClose}
          autoRunProps={{
            isRunningAll,
            autoMode,
            currentStage,
            logs,
            finalResult,
            stageResults,
            runAllStagesAuto,
          }}
        />

      case 2:
        return <Stage2Card
          udid={udid}
          onPass={() => setStage(3)}
          onBack={() => setStage(1)}
        />

      case 3:
        return <Stage3Card
          udid={udid}
          onPass={() => setStage(4)}
          onBack={() => setStage(2)}
        />

      case 4:
        return <Stage4Card
          udid={udid}
          onPass={() => setStage(5)}
          onBack={() => setStage(3)}
        />

      case 5:
        return <Stage5Card
          udid={udid}
          onPass={() => setStage(6)}
          onBack={() => setStage(4)}
        />

      case 6:
        return <Stage6Card
          udid={udid}
          onPass={() => setStage(7)}
          onBack={() => setStage(5)}
        />

      case 7:
        return <Stage7Card
          udid={udid}
          onPass={() => setStage(8)}
          onBack={() => setStage(6)}
        />

      case 8:
        return <Stage8Card
          udid={udid}
          onPass={() => setStage(9)}
          onBack={() => setStage(7)}
        />

      case 9:
        return <Stage9Card
          udid={udid}
          onPass={(r) => {
            setS9Score(r.bypass_score)
            setStage(10)
          }}
          onBack={() => setStage(8)}
        />

      case 10:
        return <Stage10Card
          udid={udid}
          stage9Score={s9Score}
          onBack={() => setStage(9)}
        />

      default:
        return null
    }
  }

  return (
    <div className="flow-container">
      {/* Global Automation Header */}
      <div className="flow-header">
        <div className="flow-title-row">
          <h1 className="flow-main-title">A12+ Signal Bypass Pipeline</h1>
          <button className="flow-close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="automation-bar">
          <button
            className={`one-click-btn ${isRunningAll ? 'running' : ''}`}
            onClick={runAllStagesAuto}
            disabled={isRunningAll}
          >
            {isRunningAll 
              ? `⚡ AUTO-RUNNING: STAGE ${currentStage}/10` 
              : "🚀 ONE-CLICK FULL AUTOMATION"}
          </button>
          
          {isRunningAll && (
            <div className="mini-progress-track">
              <div 
                className="mini-progress-fill" 
                style={{ width: `${(currentStage / 10) * 100}%` }}
              />
            </div>
          )}
        </div>
      </div>

      <div className="flow-content">
        {renderStage()}
      </div>
    </div>
  )
}
