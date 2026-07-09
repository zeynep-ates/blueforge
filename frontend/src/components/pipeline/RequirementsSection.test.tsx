import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import {
  getGetProjectVersionQueryKey,
  ProjectVersionResponseStatus,
  useRegenerateVersion,
  useUpdateRequirement,
} from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { RequirementsSection } from './RequirementsSection'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useUpdateRequirement: vi.fn(),
  useRegenerateVersion: vi.fn(),
}))

const version: ProjectVersionResponse = {
  versionId: 10,
  projectId: 1,
  versionNumber: 1,
  ideaSnapshot: 'An idea',
  status: ProjectVersionResponseStatus.REQUIREMENTS_GENERATED,
  questions: [],
  requirements: [
    { id: 200, type: 'FUNCTIONAL', title: 'Original title', description: 'Original description', orderIndex: 0 },
  ],
  epics: [],
  userStories: [],
  tasks: [],
}

function renderSection(queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RequirementsSection version={version} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RequirementsSection editing', () => {
  it('submits the edited fields and patches the cached version on success', () => {
    const mutate = vi.fn()
    vi.mocked(useUpdateRequirement).mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useUpdateRequirement>)
    vi.mocked(useRegenerateVersion).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRegenerateVersion>)

    const queryClient = new QueryClient()
    const queryKey = getGetProjectVersionQueryKey(1, 1)
    queryClient.setQueryData(queryKey, version)

    renderSection(queryClient)

    fireEvent.click(screen.getByRole('button', { name: 'Edit requirement' }))
    fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Updated title' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(mutate).toHaveBeenCalledWith(
      { requirementId: 200, data: { title: 'Updated title', description: 'Original description' } },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )

    const onSuccess = mutate.mock.calls[0][1].onSuccess
    onSuccess({ id: 200, type: 'FUNCTIONAL', title: 'Updated title', description: 'Original description', orderIndex: 0 })

    const cached = queryClient.getQueryData<ProjectVersionResponse>(queryKey)
    expect(cached?.requirements?.[0].title).toBe('Updated title')
  })
})

describe('RequirementsSection regenerating', () => {
  it('regenerates requirements and navigates to the new version on success', () => {
    const mutate = vi.fn((_vars, options: { onSuccess: (v: ProjectVersionResponse) => void }) =>
      options.onSuccess({ ...version, versionId: 11, versionNumber: 2, changeDescription: 'Tried again' }),
    )
    vi.mocked(useUpdateRequirement).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useUpdateRequirement>)
    vi.mocked(useRegenerateVersion).mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
    } as unknown as ReturnType<typeof useRegenerateVersion>)

    const queryClient = new QueryClient()
    renderSection(queryClient)

    fireEvent.click(screen.getByRole('button', { name: 'Regenerate Requirements' }))
    fireEvent.change(screen.getByLabelText('Note (optional)'), { target: { value: 'Tried again' } })
    fireEvent.click(screen.getByRole('button', { name: 'Regenerate' }))

    expect(mutate).toHaveBeenCalledWith(
      {
        projectId: 1,
        versionNumber: 1,
        data: { targetStage: 'REQUIREMENTS_GENERATED', changeDescription: 'Tried again' },
      },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
  })
})
