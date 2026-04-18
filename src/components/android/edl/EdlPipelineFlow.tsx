import { useState } from "react"

import { EdlStage1Card } from "./stage1/EdlStage1Card"
import { EdlStage2Card } from "./stage2/EdlStage2Card"
import { EdlStage3Card } from "./stage3/EdlStage3Card"
import { EdlStage4Card } from "./stage4/EdlStage4Card"
import { EdlStage5Card } from "./stage5/EdlStage5Card"
import { EdlStage6Card } from "./stage6/EdlStage6Card"
import { EdlStage7Card } from "./stage7/EdlStage7Card"
import { EdlStage8Card } from "./stage8/EdlStage8Card"
import { EdlStage9Card } from "./stage9/EdlStage9Card"
import { EdlStage10Card } from "./stage10/EdlStage10Card"
import { EdlStage11Card } from "./stage11/EdlStage11Card"
import { EdlStage12Card } from "./stage12/EdlStage12Card"
import { EdlStage13Card } from "./stage13/EdlStage13Card"
import { EdlStage14Card } from "./stage14/EdlStage14Card"
import { EdlStage15Card } from "./stage15/EdlStage15Card"
import { EdlStage16Card } from "./stage16/EdlStage16Card"
import { EdlStage17Card } from "./stage17/EdlStage17Card"
import { EdlStage18Card } from "./stage18/EdlStage18Card"
import { EdlStage19Card } from "./stage19/EdlStage19Card"
import { EdlStage20Card } from "./stage20/EdlStage20Card"
import type { EdlStage1Result, EdlPipelineStageResult } from "./types"

export function EdlPipelineFlow({
  onClose,
}: {
  onClose: () => void
}) {
  const [stage, setStage] = useState(1)
  const [selectedSerial, setSelectedSerial] = useState("")

  const advance = (_result: EdlPipelineStageResult) => {
    setStage((previous) => Math.min(previous + 1, 20))
  }

  const handleStage1Pass = (result: EdlStage1Result) => {
    setSelectedSerial(result.selected_serial)
    setStage(2)
  }

  switch (stage) {
    case 1:
      return <EdlStage1Card onPass={handleStage1Pass} onClose={onClose} />
    case 2:
      return <EdlStage2Card serial={selectedSerial} onPass={advance} onBack={() => setStage(1)} onClose={onClose} />
    case 3:
      return <EdlStage3Card serial={selectedSerial} onPass={advance} onBack={() => setStage(2)} onClose={onClose} />
    case 4:
      return <EdlStage4Card serial={selectedSerial} onPass={advance} onBack={() => setStage(3)} onClose={onClose} />
    case 5:
      return <EdlStage5Card serial={selectedSerial} onPass={advance} onBack={() => setStage(4)} onClose={onClose} />
    case 6:
      return <EdlStage6Card serial={selectedSerial} onPass={advance} onBack={() => setStage(5)} onClose={onClose} />
    case 7:
      return <EdlStage7Card serial={selectedSerial} onPass={advance} onBack={() => setStage(6)} onClose={onClose} />
    case 8:
      return <EdlStage8Card serial={selectedSerial} onPass={advance} onBack={() => setStage(7)} onClose={onClose} />
    case 9:
      return <EdlStage9Card serial={selectedSerial} onPass={advance} onBack={() => setStage(8)} onClose={onClose} />
    case 10:
      return <EdlStage10Card serial={selectedSerial} onPass={advance} onBack={() => setStage(9)} onClose={onClose} />
    case 11:
      return <EdlStage11Card serial={selectedSerial} onPass={advance} onBack={() => setStage(10)} onClose={onClose} />
    case 12:
      return <EdlStage12Card serial={selectedSerial} onPass={advance} onBack={() => setStage(11)} onClose={onClose} />
    case 13:
      return <EdlStage13Card serial={selectedSerial} onPass={advance} onBack={() => setStage(12)} onClose={onClose} />
    case 14:
      return <EdlStage14Card serial={selectedSerial} onPass={advance} onBack={() => setStage(13)} onClose={onClose} />
    case 15:
      return <EdlStage15Card serial={selectedSerial} onPass={advance} onBack={() => setStage(14)} onClose={onClose} />
    case 16:
      return <EdlStage16Card serial={selectedSerial} onPass={advance} onBack={() => setStage(15)} onClose={onClose} />
    case 17:
      return <EdlStage17Card serial={selectedSerial} onPass={advance} onBack={() => setStage(16)} onClose={onClose} />
    case 18:
      return <EdlStage18Card serial={selectedSerial} onPass={advance} onBack={() => setStage(17)} onClose={onClose} />
    case 19:
      return <EdlStage19Card serial={selectedSerial} onPass={advance} onBack={() => setStage(18)} onClose={onClose} />
    case 20:
      return <EdlStage20Card serial={selectedSerial} onBack={() => setStage(19)} onClose={onClose} />
    default:
      return null
  }
}
