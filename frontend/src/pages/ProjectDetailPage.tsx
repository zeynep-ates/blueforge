import { ArrowLeft, FileQuestion, FolderOpen, GitCompare } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useGetProject } from '@/api/generated'
import type { ProjectVersionSummaryResponse } from '@/api/generated'
import { Logo } from '@/components/Logo'
import { ModeToggle } from '@/components/mode-toggle'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { statusLabel } from '@/lib/pipeline'

function versionOptionLabel(version: ProjectVersionSummaryResponse) {
  return version.changeDescription
    ? `Version ${version.versionNumber} — ${version.changeDescription}`
    : `Version ${version.versionNumber}`
}

function CompareVersionsControl({
  projectId,
  versions,
}: {
  projectId: number
  versions: ProjectVersionSummaryResponse[]
}) {
  const navigate = useNavigate()
  const sorted = [...versions].sort((a, b) => (a.versionNumber ?? 0) - (b.versionNumber ?? 0))
  const [fromVersion, setFromVersion] = useState(String(sorted[sorted.length - 2]?.versionNumber ?? ''))
  const [toVersion, setToVersion] = useState(String(sorted[sorted.length - 1]?.versionNumber ?? ''))

  if (sorted.length < 2) {
    return null
  }

  return (
    <div className="flex flex-wrap items-end gap-2 rounded-lg border p-3">
      <div className="flex flex-col gap-1">
        <span className="text-muted-foreground text-xs">From</span>
        <Select value={fromVersion} onValueChange={setFromVersion}>
          <SelectTrigger className="w-56"><SelectValue /></SelectTrigger>
          <SelectContent>
            {sorted.map((version) => (
              <SelectItem key={version.versionId} value={String(version.versionNumber)}>
                {versionOptionLabel(version)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-muted-foreground text-xs">To</span>
        <Select value={toVersion} onValueChange={setToVersion}>
          <SelectTrigger className="w-56"><SelectValue /></SelectTrigger>
          <SelectContent>
            {sorted.map((version) => (
              <SelectItem key={version.versionId} value={String(version.versionNumber)}>
                {versionOptionLabel(version)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <Button
        className="gap-1.5"
        onClick={() => navigate(`/projects/${projectId}/diff/${fromVersion}/${toVersion}`)}
      >
        <GitCompare className="size-4" />
        Compare
      </Button>
    </div>
  )
}

export function ProjectDetailPage() {
  const navigate = useNavigate()
  const { projectId } = useParams<{ projectId: string }>()
  const projectIdNum = Number(projectId)

  const { data: project, isLoading, isError } = useGetProject(projectIdNum)

  return (
    <div className="from-background to-accent/10 min-h-svh bg-gradient-to-b">
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 p-8">
        <div className="flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-sm font-semibold">
            <Logo className="size-6" />
            BlueForge
          </Link>
          <ModeToggle />
        </div>

        <Button render={<Link to="/projects" />} variant="ghost" className="w-fit gap-1.5 px-2">
          <ArrowLeft className="size-4" />
          All projects
        </Button>

        {isLoading && (
          <div className="flex flex-col gap-6">
            <Skeleton className="h-8 w-64" />
            <div className="flex flex-col gap-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          </div>
        )}

        {(isError || (!isLoading && !project)) && (
          <div className="flex flex-col items-center gap-4 p-16 text-center">
            <FileQuestion className="text-muted-foreground size-10" />
            <p className="text-muted-foreground max-w-sm">
              Couldn't find that project. It may not exist, or the backend may be unreachable.
            </p>
            <Button render={<Link to="/projects" />}>Back to projects</Button>
          </div>
        )}

        {!isLoading && !isError && project && (
          <>
            <div className="flex flex-col gap-1.5">
              <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
                <FolderOpen className="text-primary size-6" />
                {project.name}
              </h1>
              <p className="text-muted-foreground text-sm">
                Created {project.createdAt ? new Date(project.createdAt).toLocaleDateString() : 'unknown date'}
              </p>
            </div>

            <CompareVersionsControl projectId={projectIdNum} versions={project.versions ?? []} />

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Version</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(project.versions ?? []).map((version) => (
                  <TableRow
                    key={version.versionId}
                    className="cursor-pointer"
                    onClick={() => navigate(`/projects/${projectIdNum}/versions/${version.versionNumber}`)}
                  >
                    <TableCell className="font-medium">Version {version.versionNumber}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{statusLabel(version.status)}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </>
        )}
      </div>
    </div>
  )
}
