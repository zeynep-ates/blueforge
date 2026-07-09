import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StageSection } from './StageSection'

describe('StageSection', () => {
  it('renders children and the action when unlocked', () => {
    render(
      <StageSection id="epics" title="Epics" state="current" action={<button>Generate Epics</button>}>
        <p>Epic content</p>
      </StageSection>,
    )

    expect(screen.getByText('Epics')).toBeInTheDocument()
    expect(screen.getByText('In progress')).toBeInTheDocument()
    expect(screen.getByText('Epic content')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generate Epics' })).toBeInTheDocument()
  })

  it('hides children and the action, showing the locked message instead', () => {
    render(
      <StageSection
        id="epics"
        title="Epics"
        state="locked"
        lockedMessage="Answer the clarifying questions to unlock epic generation."
        action={<button>Generate Epics</button>}
      >
        <p>Epic content</p>
      </StageSection>,
    )

    expect(screen.getByText('Locked')).toBeInTheDocument()
    expect(screen.getByText('Answer the clarifying questions to unlock epic generation.')).toBeInTheDocument()
    expect(screen.queryByText('Epic content')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Generate Epics' })).not.toBeInTheDocument()
  })

  it('renders a done badge without requiring an action', () => {
    render(
      <StageSection id="requirements" title="Requirements" state="done">
        <p>Requirement content</p>
      </StageSection>,
    )

    expect(screen.getByText('Done')).toBeInTheDocument()
    expect(screen.getByText('Requirement content')).toBeInTheDocument()
  })
})
