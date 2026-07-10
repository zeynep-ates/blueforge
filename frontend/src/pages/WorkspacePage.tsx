import { useQueryClient } from '@tanstack/react-query'
import { Download, FileQuestion, FolderOpen } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { getGetProjectVersionQueryKey, useGetProjectVersion } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { WorkspaceSidebar } from '@/components/layout/WorkspaceSidebar'
import { ArchitectureRecommendationsSection } from '@/components/pipeline/ArchitectureRecommendationsSection'
import { EpicsSection } from '@/components/pipeline/EpicsSection'
import { QuestionsSection } from '@/components/pipeline/QuestionsSection'
import { RequirementsSection } from '@/components/pipeline/RequirementsSection'
import { TasksSection } from '@/components/pipeline/TasksSection'
import { UserStoriesSection } from '@/components/pipeline/UserStoriesSection'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { downloadFile } from '@/lib/download'

function WorkspaceSkeleton() {
  return (
    <div className="flex min-h-svh w-full">
      <div className="bg-sidebar hidden w-64 shrink-0 flex-col gap-6 border-r p-4 md:flex">
        <Skeleton className="h-6 w-28" />
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-8 w-full" />
          ))}
        </div>
      </div>
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 p-6">
        <Skeleton className="h-8 w-64" />
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-40 w-full" />
        ))}
      </div>
    </div>
  )
}

export function WorkspacePage() {
  const { projectId, versionNumber } = useParams<{ projectId: string; versionNumber: string }>()
  const queryClient = useQueryClient()

  const projectIdNum = Number(projectId)
  const versionNumberNum = Number(versionNumber)

  const { data: version, isLoading, isError } = useGetProjectVersion(projectIdNum, versionNumberNum)

  function handleUpdated(updated: ProjectVersionResponse) {
    queryClient.setQueryData(getGetProjectVersionQueryKey(projectIdNum, versionNumberNum), updated)
  }

  if (isLoading) {
    return <WorkspaceSkeleton />
  }

  if (isError || !version) {
    return (
      <div className="flex min-h-svh flex-col items-center justify-center gap-4 p-16 text-center">
        <FileQuestion className="text-muted-foreground size-10" />
        <p className="text-muted-foreground max-w-sm">
          Couldn't find that project version. It may not exist, or the backend may be unreachable.
        </p>
        <Button render={<Link to="/" />}>Start a new idea</Button>
      </div>
    )
  }

  return (
    <SidebarProvider>
      <WorkspaceSidebar version={version} />
      <SidebarInset>
        <header className="bg-background/95 sticky top-0 z-10 flex items-center gap-3 border-b px-4 py-3 backdrop-blur">
          <SidebarTrigger />
          <FolderOpen className="text-primary size-4 shrink-0" />
          <div className="flex min-w-0 flex-1 flex-col">
            <span className="text-sm font-medium">
              Project {version.projectId} · Version {version.versionNumber}
            </span>
            <span className="text-muted-foreground line-clamp-1 text-xs">{version.ideaSnapshot}</span>
            {version.versionNumber! > 1 && (
              <span className="text-muted-foreground line-clamp-1 text-xs italic">
                Regenerated{version.changeDescription ? `: ${version.changeDescription}` : ''}
              </span>
            )}
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger
              render={
                <Button variant="outline" size="sm">
                  <Download className="size-4" />
                  Export
                </Button>
              }
            />
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                onClick={() =>
                  downloadFile(
                    `/api/projects/${projectIdNum}/versions/${versionNumberNum}/export?format=markdown`,
                  ).catch((error: unknown) => console.error('Failed to download Markdown export', error))
                }
              >
                Markdown (.md)
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() =>
                  downloadFile(
                    `/api/projects/${projectIdNum}/versions/${versionNumberNum}/export?format=json`,
                  ).catch((error: unknown) => console.error('Failed to download JSON export', error))
                }
              >
                JSON (.json)
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>
        <div className="animate-in fade-in mx-auto flex w-full max-w-3xl flex-col gap-6 p-6 duration-300">
          <QuestionsSection version={version} onUpdated={handleUpdated} />
          <RequirementsSection version={version} />
          <EpicsSection version={version} onUpdated={handleUpdated} />
          <UserStoriesSection version={version} onUpdated={handleUpdated} />
          <TasksSection version={version} onUpdated={handleUpdated} />
          <ArchitectureRecommendationsSection version={version} onUpdated={handleUpdated} />
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
