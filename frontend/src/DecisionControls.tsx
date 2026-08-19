import { useState } from 'react'
import './DecisionControls.css'

export type DecisionStatus = 'APPROVED' | 'RECHECK' | 'HOLD'

type Props = {
  disabled?: boolean
  onDecide: (status: DecisionStatus, reason: string) => Promise<void>
}

export default function DecisionControls({ disabled = false, onDecide }: Props) {
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)

  async function decide(status: DecisionStatus) {
    setSaving(true)
    try {
      await onDecide(status, reason)
      setReason('')
    } finally {
      setSaving(false)
    }
  }

  return <div className="decision-controls">
    <input aria-label="판단 사유" value={reason} maxLength={500} placeholder="판단 사유(선택)"
      onChange={(event) => setReason(event.target.value)} />
    <div>
      <button disabled={disabled || saving} onClick={() => decide('APPROVED')}>정상 처리</button>
      <button disabled={disabled || saving} onClick={() => decide('RECHECK')}>재확인</button>
      <button disabled={disabled || saving} onClick={() => decide('HOLD')}>보류</button>
    </div>
  </div>
}
