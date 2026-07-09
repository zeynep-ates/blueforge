import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ProjectVersionResponseStatus, useGenerateEpics, useRegenerateVersion, useUpdateEpic } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { EpicsSection } from './EpicsSection'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useGenerateEpics: vi.fn(),
  useUpdateEpic: vi.fn(),
  useRegenerateVersion: vi.fn(),
}))

const doneVersion: ProjectVersionResponse = {
  versionId: 10,
  projectId: 1,
  versionNumber: 1,
  ideaSnapshot: 'An idea',
  status: ProjectVersionResponseStatus.USER_STORIES_GENERATED,
  questions: [],
  requirements: [],
  epics: [{ id: 300, title: 'Onboarding', description: 'Covers onboarding.', orderIndex: 0 }],
  userStories: [],
  tasks: [],
}

const currentVersion: ProjectVersionResponse = { ...doneVersion, status: ProjectVersionResponseStatus.REQUIREMENTS_GENERATED }

function renderSection(version: ProjectVersionResponse) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <EpicsSection version={version} onUpdated={vi.fn()} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function mockHooks() {
  vi.mocked(useGenerateEpics).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
  } as unknown as ReturnType<typeof useGenerateEpics>)
  vi.mocked(useUpdateEpic).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
  } as unknown as ReturnType<typeof useUpdateEpic>)
}

describe('EpicsSection regenerating', () => {
  it('shows Generate (not Regenerate) while the stage is still current', () => {
    mockHooks()
    vi.mocked(useRegenerateVersion).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRegenerateVersion>)

    renderSection(currentVersion)

    expect(screen.getByRole('button', { name: 'Generate Epics' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Regenerate Epics' })).not.toBeInTheDocument()
  })

  it('regenerates epics and navigates to the new version on success once the stage is done', () => {
    mockHooks()
    const mutate = vi.fn((_vars, options: { onSuccess: (v: ProjectVersionResponse) => void }) =>
      options.onSuccess({ ...doneVersion, versionId: 11, versionNumber: 2 }),
    )
    vi.mocked(useRegenerateVersion).mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRegenerateVersion>)

    renderSection(doneVersion)

    expect(screen.queryByRole('button', { name: 'Generate Epics' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Epics' }))
    fireEvent.click(screen.getByRole('button', { name: 'Regenerate' }))

    expect(mutate).toHaveBeenCalledWith(
      { projectId: 1, versionNumber: 1, data: { targetStage: 'EPICS_GENERATED', changeDescription: undefined } },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
  })
})
