type Csrf = { token: string; headerName: string }

async function csrf(): Promise<Csrf> {
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  if (!response.ok) throw new Error('보안 토큰을 가져오지 못했습니다.')
  return response.json()
}

async function message(response: Response): Promise<string> {
  const body = await response.json().catch(() => ({}))
  return body.message ?? '요청을 처리하지 못했습니다.'
}

export async function register(email: string, password: string) {
  const token = await csrf()
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', [token.headerName]: token.token },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) throw new Error(await message(response))
}

export async function login(email: string, password: string) {
  const token = await csrf()
  const form = new URLSearchParams({ username: email, password })
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', [token.headerName]: token.token },
    body: form,
  })
  if (!response.ok) throw new Error(await message(response))
}

export async function currentUser() {
  const response = await fetch('/api/auth/me', { credentials: 'include' })
  if (response.status === 401) return null
  if (!response.ok) throw new Error(await message(response))
  return response.json() as Promise<{ email: string; role: string }>
}

export async function logout() {
  const token = await csrf()
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { [token.headerName]: token.token },
  })
  if (!response.ok) throw new Error(await message(response))
}

export type Policy = {
  id: number
  title: string
  versionNo: number
  originalFilename: string
  status: string
  registeredBy: string
  createdAt: string
}

export async function policies(): Promise<Policy[]> {
  const response = await fetch('/api/admin/policies', { credentials: 'include' })
  if (!response.ok) throw new Error(await message(response))
  return response.json()
}

export async function uploadPolicy(title: string, file: File): Promise<Policy> {
  const token = await csrf()
  const form = new FormData()
  form.append('title', title)
  form.append('file', file)
  const response = await fetch('/api/admin/policies', {
    method: 'POST',
    credentials: 'include',
    headers: { [token.headerName]: token.token },
    body: form,
  })
  if (!response.ok) throw new Error(await message(response))
  return response.json()
}

export type BatchCreated = { batchId: number; originalFilename: string; status: string; totalCount: number; violationCount: number; explanationCount: number }

export async function uploadBatch(file: File): Promise<BatchCreated> {
  const token = await csrf()
  const form = new FormData()
  form.append('file', file)
  const response = await fetch('/api/review-batches', {
    method: 'POST', credentials: 'include', headers: { [token.headerName]: token.token }, body: form,
  })
  if (!response.ok) throw new Error(await message(response))
  return response.json()
}

export type BatchDetail = BatchCreated & {
  createdAt: string
  transactions: Array<{
    id: number
    transactionId: string
    merchant: string
    amount: number
    reviewStatus: string
    violations: Array<{ ruleCode: string; reason: string }>
    explanation: null | { summary: string; citations: Array<Record<string, unknown>>; generatedBy: string }
  }>
}

export async function getBatch(batchId: number): Promise<BatchDetail> {
  const response = await fetch(`/api/review-batches/${batchId}`, { credentials: 'include' })
  if (!response.ok) throw new Error(await message(response))
  return response.json()
}
