import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import DecisionControls from './DecisionControls'

describe('DecisionControls', () => {
  it('sends the selected status and reason', async () => {
    const onDecide = vi.fn().mockResolvedValue(undefined)
    render(<DecisionControls onDecide={onDecide} />)

    fireEvent.change(screen.getByLabelText('판단 사유'), { target: { value: '증빙 확인 완료' } })
    fireEvent.click(screen.getByRole('button', { name: '정상 처리' }))

    await waitFor(() => expect(onDecide).toHaveBeenCalledWith('APPROVED', '증빙 확인 완료'))
    expect(screen.getByLabelText('판단 사유')).toHaveValue('')
  })
})
