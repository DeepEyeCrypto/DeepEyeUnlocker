import { useState } from "react"
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

export function SignalBypassFlow({
  onClose,
}: {
  onClose: () => void
}) {
  const [stage, setStage]       = useState(1)
  const [udid, setUdid]         = useState("")
  const [s9Score, setS9Score]   = useState(0)

  // Each stage's onPass advances to next stage
  // onBack goes to previous stage

  switch (stage) {
    case 1:
      return <Stage1Card
        onPass={(r) => {
          setUdid(r.udid)
          setStage(2)
        }}
        onClose={onClose}
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
