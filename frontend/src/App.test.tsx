import { render, screen, waitFor } from '@testing-library/react'
import { expect, test, vi } from 'vitest'
import App from './App'

vi.mock('./api', () => ({
  currentUser: vi.fn().mockResolvedValue(null),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}))

test('로그인과 회원가입 진입점을 표시함', async () => {
  render(<App />)
  await waitFor(() => expect(screen.getAllByRole('button', { name: '로그인' })).toHaveLength(2))
  expect(screen.getByRole('button', { name: '회원가입' })).toBeInTheDocument()
  expect(screen.getByLabelText('이메일')).toBeInTheDocument()
})
