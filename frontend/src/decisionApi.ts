import { DecisionStatus } from './DecisionControls'

type Csrf = { token: string; headerName: string }

async function csrf(): Promise<Csrf> {
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  if (!response.ok) throw new Error('보안 토큰을 가져오지 못했습니다.')
  return response.json()
}

export async function saveDecision(batchId: number, transactionId: number, status: DecisionStatus, reason: string) {
  const token = await csrf()
  const response = await fetch(`/api/review-batches/${batchId}/transactions/${transactionId}/decisions`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', [token.headerName]: token.token },
    body: JSON.stringify({ status, reason: reason || null }),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message ?? '담당자 판단을 저장하지 못했습니다.')
  }
  return response.json()
}
