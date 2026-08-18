import { FormEvent, useEffect, useState } from 'react'
import { currentUser, login, logout, register } from './api'
import './style.css'

type Mode = 'login' | 'register'
type User = { email: string; role: string }

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [mode, setMode] = useState<Mode>('login')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    currentUser().then(setUser).catch((error) => setMessage(error.message)).finally(() => setLoading(false))
  }, [])

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

  if (loading) return <main className="auth-shell"><p>로그인 상태 확인 중…</p></main>

  if (user) return (
    <main className="auth-shell">
      <section className="card welcome">
        <span className="eyebrow">AI SETTLEMENT REVIEW</span>
        <h1>{user.email}</h1>
        <p>{user.role === 'ADMIN' ? '관리자' : '검수 담당자'}로 로그인됨</p>
        <button onClick={async () => { await logout(); setUser(null) }}>로그아웃</button>
      </section>
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
