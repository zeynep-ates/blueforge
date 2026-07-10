import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import {
  ProjectVersionResponseStatus,
  useGenerateArchitectureRecommendations,
  useRegenerateVersion,
} from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { ArchitectureRecommendationsSection } from './ArchitectureRecommendationsSection'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useGenerateArchitectureRecommendations: vi.fn(),
  useRegenerateVersion: vi.fn(),
}))

const version: ProjectVersionResponse = {
  versionId: 10,
  projectId: 1,
  versionNumber: 1,
  ideaSnapshot: 'An idea',
  status: ProjectVersionResponseStatus.TASKS_GENERATED,
  questions: [],
  requirements: [],
  epics: [],
  userStories: [],
  tasks: [],
  architectureRecommendations: [],
}

function renderSection(queryClient: QueryClient, onUpdated = vi.fn()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ArchitectureRecommendationsSection version={version} onUpdated={onUpdated} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ArchitectureRecommendationsSection generating', () => {
  it('generates architecture recommendations and patches the cache on success', () => {
    const mutate = vi.fn((_vars, options: { onSuccess: (v: ProjectVersionResponse) => void }) =>
      options.onSuccess({
        ...version,
        status: ProjectVersionResponseStatus.ARCHITECTURE_GENERATED,
        architectureRecommendations: [
          {
            id: 600,
            component: 'Backend Framework',
            recommendation: 'Spring Boot',
            reasoning: 'Fits the requirements.',
            tradeoffs: 'Serverless was considered but rejected.',
            orderIndex: 0,
          },
        ],
      }),
    )
    vi.mocked(useGenerateArchitectureRecommendations).mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useGenerateArchitectureRecommendations>)
    vi.mocked(useRegenerateVersion).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRegenerateVersion>)

    const onUpdated = vi.fn()
    const queryClient = new QueryClient()
    renderSection(queryClient, onUpdated)

    fireEvent.click(screen.getByRole('button', { name: 'Generate Architecture Recommendations' }))

    expect(mutate).toHaveBeenCalledWith(
      { projectId: 1, versionNumber: 1 },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
    expect(onUpdated).toHaveBeenCalledWith(
      expect.objectContaining({ status: ProjectVersionResponseStatus.ARCHITECTURE_GENERATED }),
    )
  })
})

describe('ArchitectureRecommendationsSection regenerating', () => {
  it('regenerates architecture recommendations and navigates to the new version on success', () => {
    const doneVersion: ProjectVersionResponse = {
      ...version,
      status: ProjectVersionResponseStatus.ARCHITECTURE_GENERATED,
      architectureRecommendations: [
        {
          id: 600,
          component: 'Database',
          recommendation: 'PostgreSQL',
          reasoning: 'Relational domain.',
          tradeoffs: 'MongoDB was considered.',
          orderIndex: 0,
        },
      ],
    }
    const mutate = vi.fn((_vars, options: { onSuccess: (v: ProjectVersionResponse) => void }) =>
      options.onSuccess({ ...doneVersion, versionId: 11, versionNumber: 2, changeDescription: 'Tried again' }),
    )
    vi.mocked(useGenerateArchitectureRecommendations).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useGenerateArchitectureRecommendations>)
    vi.mocked(useRegenerateVersion).mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRegenerateVersion>)

    const queryClient = new QueryClient()
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ArchitectureRecommendationsSection version={doneVersion} onUpdated={vi.fn()} />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Architecture Recommendations' }))
    fireEvent.change(screen.getByLabelText('Note (optional)'), { target: { value: 'Tried again' } })
    fireEvent.click(screen.getByRole('button', { name: 'Regenerate' }))

    expect(mutate).toHaveBeenCalledWith(
      {
        projectId: 1,
        versionNumber: 1,
        data: { targetStage: 'ARCHITECTURE_GENERATED', changeDescription: 'Tried again' },
      },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
  })
})
