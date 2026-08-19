import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App'
import { currentUser, getBatch, uploadBatch } from './api'

vi.mock('./api', () => ({
  currentUser: vi.fn().mockResolvedValue(null),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  policies: vi.fn().mockResolvedValue([]),
  uploadPolicy: vi.fn(),
  uploadBatch: vi.fn(),
  getBatch: vi.fn(),
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

test('일반 사용자에게 CSV 배치 생성 화면을 표시함', async () => {
  vi.mocked(currentUser).mockResolvedValue({ email: 'user@example.com', role: 'USER' })
  render(<App />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'CSV 배치 생성' })).toBeInTheDocument())
  expect(screen.getByLabelText('정산 CSV')).toBeInTheDocument()
})

test('CSV 생성 후 거래별 검수 결과를 표시함', async () => {
  vi.mocked(currentUser).mockResolvedValue({ email: 'user@example.com', role: 'USER' })
  vi.mocked(uploadBatch).mockResolvedValue({ batchId: 1, originalFilename: 'sample.csv', status: 'COMPLETED', totalCount: 1, violationCount: 1, explanationCount: 1 })
  vi.mocked(getBatch).mockResolvedValue({
    batchId: 1, originalFilename: 'sample.csv', status: 'COMPLETED', totalCount: 1,
    violationCount: 1, explanationCount: 1, createdAt: '2026-08-19T08:00:00',
    transactions: [{ id: 1, transactionId: 'T-001', merchant: 'ABC상사', amount: 1250000,
      reviewStatus: 'PENDING', violations: [{ ruleCode: 'AMOUNT_EXCEEDED', reason: '고액 거래임' }],
      explanation: { summary: '정책 근거 설명', citations: [{}], generatedBy: 'OPENAI_RAG' } }],
  })
  render(<App />)
  await waitFor(() => expect(screen.getByRole('button', { name: 'CSV 배치 생성' })).toBeInTheDocument())

  fireEvent.change(screen.getByLabelText('정산 CSV'), { target: { files: [new File(['csv'], 'sample.csv')] } })
  fireEvent.submit(screen.getByRole('button', { name: 'CSV 배치 생성' }).closest('form')!)

  await waitFor(() => expect(screen.getByText('정책 근거 설명')).toBeInTheDocument())
  expect(screen.getByText('AMOUNT_EXCEEDED')).toBeInTheDocument()
})
