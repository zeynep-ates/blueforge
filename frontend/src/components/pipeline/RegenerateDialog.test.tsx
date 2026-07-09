import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { RegenerateDialog } from './RegenerateDialog'

describe('RegenerateDialog', () => {
  it('calls onRegenerate with the trimmed note and closes on completion', () => {
    const onRegenerate = vi.fn((_note, onStarted: () => void) => onStarted())
    render(
      <RegenerateDialog stageLabel="Epics" onRegenerate={onRegenerate} isPending={false} isError={false} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Epics' }))
    fireEvent.change(screen.getByLabelText('Note (optional)'), { target: { value: '  Tried again  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Regenerate' }))

    expect(onRegenerate).toHaveBeenCalledWith('Tried again', expect.any(Function))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('passes undefined when no note is entered', () => {
    const onRegenerate = vi.fn((_note, onStarted: () => void) => onStarted())
    render(
      <RegenerateDialog stageLabel="Epics" onRegenerate={onRegenerate} isPending={false} isError={false} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Epics' }))
    fireEvent.click(screen.getByRole('button', { name: 'Regenerate' }))

    expect(onRegenerate).toHaveBeenCalledWith(undefined, expect.any(Function))
  })

  it('shows an error message when isError is true', () => {
    render(<RegenerateDialog stageLabel="Epics" onRegenerate={vi.fn()} isPending={false} isError={true} />)

    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Epics' }))

    expect(screen.getByText('Failed to regenerate. Please try again.')).toBeInTheDocument()
  })

  it('disables the confirm button and shows a spinner label while pending', () => {
    render(<RegenerateDialog stageLabel="Epics" onRegenerate={vi.fn()} isPending={true} isError={false} />)

    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Epics' }))

    expect(screen.getByRole('button', { name: 'Regenerating...' })).toBeDisabled()
  })
})
