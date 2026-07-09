import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useListProjects } from '@/api/generated'
import { ProjectsListPage } from './ProjectsListPage'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useListProjects: vi.fn(),
}))

function renderPage() {
  const queryClient = new QueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ProjectsListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProjectsListPage', () => {
  it('renders a row per project with its latest version and status', () => {
    vi.mocked(useListProjects).mockReturnValue({
      data: [
        {
          id: 1,
          name: 'Recipe Sharing App',
          createdAt: '2026-07-01T00:00:00Z',
          latestVersionNumber: 2,
          latestStatus: 'TASKS_GENERATED',
        },
      ],
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useListProjects>)

    renderPage()

    expect(screen.getByText('Recipe Sharing App')).toBeInTheDocument()
    expect(screen.getByText('v2')).toBeInTheDocument()
    expect(screen.getByText('Tasks generated')).toBeInTheDocument()
  })

  it('shows an empty state when there are no projects', () => {
    vi.mocked(useListProjects).mockReturnValue({
      data: [],
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useListProjects>)

    renderPage()

    expect(screen.getByText('No projects yet. Start a new idea to begin planning.')).toBeInTheDocument()
  })

  it('shows an error message when the request fails', () => {
    vi.mocked(useListProjects).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as ReturnType<typeof useListProjects>)

    renderPage()

    expect(screen.getByText('Failed to load projects. Please try again.')).toBeInTheDocument()
  })
})
