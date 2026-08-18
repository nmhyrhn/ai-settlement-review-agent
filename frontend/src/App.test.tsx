import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App'
import { currentUser } from './api'

vi.mock('./api', () => ({
  currentUser: vi.fn().mockResolvedValue(null),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  policies: vi.fn().mockResolvedValue([]),
  uploadPolicy: vi.fn(),
}))

afterEach(() => {
  cleanup()
  vi.mocked(currentUser).mockResolvedValue(null)
})

test('로그인과 회원가입 진입점을 표시함', async () => {
  render(<App />)
  await waitFor(() => expect(screen.getAllByRole('button', { name: '로그인' })).toHaveLength(2))
  expect(screen.getByRole('button', { name: '회원가입' })).toBeInTheDocument()
  expect(screen.getByLabelText('이메일')).toBeInTheDocument()
})

test('관리자에게 정책 문서 등록 화면을 표시함', async () => {
  vi.mocked(currentUser).mockResolvedValue({ email: 'admin@example.com', role: 'ADMIN' })
  render(<App />)

  await waitFor(() => expect(screen.getByRole('heading', { name: '정산 정책 관리' })).toBeInTheDocument())
  expect(screen.getByRole('button', { name: '정책 문서 등록' })).toBeInTheDocument()
})
