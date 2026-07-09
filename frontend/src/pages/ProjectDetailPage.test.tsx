import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useGetProject } from '@/api/generated'
import { ProjectDetailPage } from './ProjectDetailPage'

const navigate = vi.fn()

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useGetProject: vi.fn(),
}))

vi.mock('react-router-dom', async (importOriginal) => ({
  ...(await importOriginal<typeof import('react-router-dom')>()),
  useNavigate: () => navigate,
}))

function renderPage(projectId = '1') {
  const queryClient = new QueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/projects/${projectId}`]}>
        <Routes>
          <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProjectDetailPage', () => {
  it('renders the project name and a row per version', () => {
    vi.mocked(useGetProject).mockReturnValue({
      data: {
        id: 1,
        name: 'Recipe Sharing App',
        createdAt: '2026-07-01T00:00:00Z',
        versions: [
          { versionId: 10, versionNumber: 1, status: 'TASKS_GENERATED' },
          { versionId: 11, versionNumber: 2, status: 'AWAITING_ANSWERS' },
        ],
      },
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useGetProject>)

    renderPage()

    expect(screen.getByText('Recipe Sharing App')).toBeInTheDocument()
    expect(screen.getByText('Version 1')).toBeInTheDocument()
    expect(screen.getByText('Version 2')).toBeInTheDocument()
    expect(screen.getByText('Tasks generated')).toBeInTheDocument()
    expect(screen.getByText('Awaiting answers')).toBeInTheDocument()
  })

  it('shows a not-found message when the project is missing', () => {
    vi.mocked(useGetProject).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as ReturnType<typeof useGetProject>)

    renderPage()

    expect(screen.getByText(/Couldn't find that project/)).toBeInTheDocument()
  })

  it('hides the compare control when there is only one version', () => {
    vi.mocked(useGetProject).mockReturnValue({
      data: {
        id: 1,
        name: 'Recipe Sharing App',
        createdAt: '2026-07-01T00:00:00Z',
        versions: [{ versionId: 10, versionNumber: 1, status: 'TASKS_GENERATED' }],
      },
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useGetProject>)

    renderPage()

    expect(screen.queryByRole('button', { name: /Compare/ })).not.toBeInTheDocument()
  })

  it('navigates to the diff page for the two most recent versions by default', () => {
    vi.mocked(useGetProject).mockReturnValue({
      data: {
        id: 1,
        name: 'Recipe Sharing App',
        createdAt: '2026-07-01T00:00:00Z',
        versions: [
          { versionId: 10, versionNumber: 1, status: 'TASKS_GENERATED' },
          { versionId: 11, versionNumber: 2, status: 'TASKS_GENERATED', changeDescription: 'Regenerated from v1' },
        ],
      },
      isLoading: false,
      isError: false,
    } as ReturnType<typeof useGetProject>)

    renderPage()

    fireEvent.click(screen.getByRole('button', { name: /Compare/ }))

    expect(navigate).toHaveBeenCalledWith('/projects/1/diff/1/2')
  })
})
