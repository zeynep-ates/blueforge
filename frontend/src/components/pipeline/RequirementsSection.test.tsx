import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { getGetProjectVersionQueryKey, ProjectVersionResponseStatus, useUpdateRequirement } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { RequirementsSection } from './RequirementsSection'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useUpdateRequirement: vi.fn(),
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
      <RequirementsSection version={version} />
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
