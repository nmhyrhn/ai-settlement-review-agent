import { FormEvent, useEffect, useState } from 'react'
import { BatchCreated, currentUser, login, logout, policies, Policy, register, uploadBatch, uploadPolicy } from './api'
import './style.css'

type Mode = 'login' | 'register'
type User = { email: string; role: string }

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [mode, setMode] = useState<Mode>('login')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [documents, setDocuments] = useState<Policy[]>([])
  const [createdBatch, setCreatedBatch] = useState<BatchCreated | null>(null)

  useEffect(() => {
    currentUser().then(setUser).catch((error) => setMessage(error.message)).finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (user?.role === 'ADMIN') policies().then(setDocuments).catch((error) => setMessage(error.message))
  }, [user])

  async function submitPolicy(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const file = form.get('file')
    if (!(file instanceof File)) return
    setMessage('정책 문서 처리 중…')
    try {
      await uploadPolicy(String(form.get('title')), file)
      setDocuments(await policies())
      formElement.reset()
      setMessage('정책 문서를 등록했음')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '정책 문서를 등록하지 못했습니다.')
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const email = String(form.get('email'))
    const password = String(form.get('password'))
    setMessage('')
    try {
      if (mode === 'register') await register(email, password)
      await login(email, password)
      setUser(await currentUser())
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '요청을 처리하지 못했습니다.')
    }
  }

  async function submitBatch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const file = new FormData(event.currentTarget).get('file')
    if (!(file instanceof File)) return
    setMessage('CSV 거래 저장 중…')
    try {
      setCreatedBatch(await uploadBatch(file))
      setMessage('검수 배치를 생성했음')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '검수 배치를 생성하지 못했습니다.')
    }
  }

  if (loading) return <main className="auth-shell"><p>로그인 상태 확인 중…</p></main>

  if (user) return (
    <main className="dashboard">
      <header><div><span className="eyebrow">AI SETTLEMENT REVIEW</span><strong>{user.email}</strong></div><button onClick={async () => { await logout(); setUser(null) }}>로그아웃</button></header>
      <section className="dashboard-title"><p>{user.role === 'ADMIN' ? '관리자' : '검수 담당자'}</p><h1>{user.role === 'ADMIN' ? '정산 정책 관리' : '정산 검수'}</h1></section>
      {user.role === 'ADMIN' ? <>
        <form className="card policy-form" onSubmit={submitPolicy}>
          <label>정책명<input name="title" placeholder="예: 국내 정산 운영 규정" required /></label>
          <label>정책 파일<input name="file" type="file" accept=".pdf,.md,.txt" required /></label>
          <button className="primary">정책 문서 등록</button>
        </form>
        {message && <p role="status" className={message.includes('못') ? 'error' : 'notice'}>{message}</p>}
        <section className="card policy-list"><h2>등록 문서</h2>{documents.length === 0 ? <p>등록된 정책 문서가 없음</p> : <table><thead><tr><th>정책</th><th>버전</th><th>상태</th><th>등록자</th></tr></thead><tbody>{documents.map(document => <tr key={document.id}><td><b>{document.title}</b><small>{document.originalFilename}</small></td><td>v{document.versionNo}</td><td><span className={`badge ${document.status.toLowerCase()}`}>{document.status}</span></td><td>{document.registeredBy}</td></tr>)}</tbody></table>}</section>
      </> : <>
        <form className="card batch-form" onSubmit={submitBatch}>
          <div><h2>새 검수 배치</h2><p>정산 CSV를 올리면 거래를 검수 배치에 저장함</p></div>
          <label>정산 CSV<input name="file" type="file" accept=".csv,text/csv" required /></label>
          <button className="primary">CSV 배치 생성</button>
        </form>
        {message && <p role="status" className={message.includes('못') ? 'error' : 'notice'}>{message}</p>}
        {createdBatch && <section className="card batch-created"><span className="badge processing">{createdBatch.status}</span><h2>배치 #{createdBatch.batchId}</h2><p>{createdBatch.originalFilename} · 거래 {createdBatch.totalCount}건 저장됨</p></section>}
      </>}
    </main>
  )

  return (
    <main className="auth-shell">
      <section className="intro">
        <span className="eyebrow">AI SETTLEMENT REVIEW</span>
        <h1>정산 검수를<br />근거 있는 판단으로</h1>
        <p>Java 규칙과 사내 정책 기반 AI 설명으로 거래 검수 과정을 기록함</p>
      </section>
      <section className="card">
        <div className="tabs">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>로그인</button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>회원가입</button>
        </div>
        <form onSubmit={submit}>
          <label>이메일<input name="email" type="email" autoComplete="email" required /></label>
          <label>비밀번호<input name="password" type="password" minLength={8} maxLength={72} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required /></label>
          {message && <p role="alert" className="error">{message}</p>}
          <button className="primary">{mode === 'login' ? '로그인' : '가입하고 시작하기'}</button>
        </form>
      </section>
    </main>
  )
}
