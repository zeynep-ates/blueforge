import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useGetVersionDiff } from '@/api/generated'
import { VersionDiffPage } from './VersionDiffPage'

vi.mock('@/api/generated', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/generated')>()),
  useGetVersionDiff: vi.fn(),
}))

function renderPage() {
  const queryClient = new QueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/projects/1/diff/1/2']}>
        <Routes>
          <Route path="/projects/:projectId/diff/:fromVersion/:toVersion" element={<VersionDiffPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const baseDiff = {
  projectId: 1,
  fromVersionNumber: 1,
  toVersionNumber: 2,
  summary: { addedCount: 1, removedCount: 0, modifiedCount: 1, unchangedCount: 1 },
  requirements: [
    {
      changeType: 'UNCHANGED' as const,
      before: { id: 200, type: 'FUNCTIONAL' as const, title: 'Same requirement', description: 'Same', orderIndex: 0 },
      after: { id: 300, type: 'FUNCTIONAL' as const, title: 'Same requirement', description: 'Same', orderIndex: 0 },
    },
    {
      changeType: 'ADDED' as const,
      before: undefined,
      after: { id: 301, type: 'FUNCTIONAL' as const, title: 'New requirement', description: 'Fresh', orderIndex: 1 },
    },
  ],
  epics: [
    {
      changeType: 'MODIFIED' as const,
      before: { id: 400, title: 'Onboarding', description: 'Old summary', orderIndex: 0 },
      after: { id: 500, title: 'Onboarding', description: 'New summary', orderIndex: 0 },
      userStories: [
        {
          changeType: 'UNCHANGED' as const,
          before: {
            id: 401,
            epicId: 400,
            title: 'Sign up',
            description: 'Desc',
            acceptanceCriteria: '- criteria',
            orderIndex: 0,
          },
          after: {
            id: 501,
            epicId: 500,
            title: 'Sign up',
            description: 'Desc',
            acceptanceCriteria: '- criteria',
            orderIndex: 0,
          },
          tasks: [
            {
              changeType: 'UNCHANGED' as const,
              before: {
                id: 402,
                userStoryId: 401,
                title: 'Build form',
                description: 'Desc',
                priority: 'HIGH' as const,
                effortEstimate: 'M' as const,
                orderIndex: 0,
              },
              after: {
                id: 502,
                userStoryId: 501,
                title: 'Build form',
                description: 'Desc',
                priority: 'HIGH' as const,
                effortEstimate: 'M' as const,
                orderIndex: 0,
              },
            },
          ],
        },
      ],
    },
  ],
  architectureRecommendations: [
    {
      changeType: 'MODIFIED' as const,
      before: {
        id: 600,
        component: 'Database',
        recommendation: 'PostgreSQL',
        reasoning: 'Relational domain.',
        tradeoffs: 'MongoDB was considered.',
        orderIndex: 0,
      },
      after: {
        id: 700,
        component: 'Database',
        recommendation: 'PostgreSQL',
        reasoning: 'Relational domain.',
        tradeoffs: 'DynamoDB was considered.',
        orderIndex: 0,
      },
    },
  ],
}

describe('VersionDiffPage', () => {
  it('renders the summary counts and version header', () => {
    vi.mocked(useGetVersionDiff).mockReturnValue({
      data: baseDiff,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useGetVersionDiff>)

    renderPage()

    expect(screen.getByText('Version 1 → Version 2')).toBeInTheDocument()
    expect(screen.getByText('1 added')).toBeInTheDocument()
    expect(screen.getByText('0 removed')).toBeInTheDocument()
    expect(screen.getByText('1 modified')).toBeInTheDocument()
    expect(screen.getByText('1 unchanged')).toBeInTheDocument()
  })

  it('renders requirement rows with their change type', () => {
    vi.mocked(useGetVersionDiff).mockReturnValue({
      data: baseDiff,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useGetVersionDiff>)

    renderPage()

    expect(screen.getByText('Same requirement')).toBeInTheDocument()
    expect(screen.getByText('New requirement')).toBeInTheDocument()
  })

  it('renders nested epic, user story, and task diffs', () => {
    vi.mocked(useGetVersionDiff).mockReturnValue({
      data: baseDiff,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useGetVersionDiff>)

    renderPage()

    expect(screen.getByText('Onboarding')).toBeInTheDocument()
    expect(screen.getByText('Sign up')).toBeInTheDocument()
    expect(screen.getByText('Build form')).toBeInTheDocument()
    expect(screen.getByText('Old summary')).toBeInTheDocument()
    expect(screen.getByText('New summary')).toBeInTheDocument()
  })

  it('renders architecture recommendation rows with their change type', () => {
    vi.mocked(useGetVersionDiff).mockReturnValue({
      data: baseDiff,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useGetVersionDiff>)

    renderPage()

    expect(screen.getByText('Architecture Recommendations')).toBeInTheDocument()
    expect(screen.getByText('MongoDB was considered.')).toBeInTheDocument()
    expect(screen.getByText('DynamoDB was considered.')).toBeInTheDocument()
  })

  it('shows a placeholder when there are no architecture recommendations in either version', () => {
    vi.mocked(useGetVersionDiff).mockReturnValue({
      data: { ...baseDiff, architectureRecommendations: [] },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useGetVersionDiff>)

    renderPage()

    expect(screen.getByText('No architecture recommendations in either version.')).toBeInTheDocument()
  })

  it('shows an error state when the diff fails to load', () => {
    vi.mocked(useGetVersionDiff).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as unknown as ReturnType<typeof useGetVersionDiff>)

    renderPage()

    expect(screen.getByText(/Couldn't compare those versions/)).toBeInTheDocument()
  })
})
