import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useGetProject } from '@/api/generated'
import { ProjectDetailPage } from './ProjectDetailPage'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useGetProject: vi.fn(),
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
})
