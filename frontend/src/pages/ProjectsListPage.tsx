import { FolderOpen, Lightbulb, Plus } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useListProjects } from '@/api/generated'
import { Logo } from '@/components/Logo'
import { ModeToggle } from '@/components/mode-toggle'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { statusLabel } from '@/lib/pipeline'

export function ProjectsListPage() {
  const navigate = useNavigate()
  const { data: projects, isLoading, isError } = useListProjects()

  return (
    <div className="from-background to-accent/10 min-h-svh bg-gradient-to-b">
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 p-8">
        <div className="flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-sm font-semibold">
            <Logo className="size-6" />
            BlueForge
          </Link>
          <div className="flex items-center gap-2">
            <Button render={<Link to="/" />} variant="outline" className="gap-1.5">
              <Plus className="size-4" />
              New idea
            </Button>
            <ModeToggle />
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-semibold tracking-tight">Projects</h1>
          <p className="text-muted-foreground">Browse every project and jump back into its latest version.</p>
        </div>

        {isLoading && (
          <div className="flex flex-col gap-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        )}

        {isError && <p className="text-destructive text-sm">Failed to load projects. Please try again.</p>}

        {!isLoading && !isError && projects && projects.length === 0 && (
          <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed p-16 text-center">
            <Lightbulb className="text-muted-foreground size-8" />
            <p className="text-muted-foreground max-w-sm">No projects yet. Start a new idea to begin planning.</p>
            <Button render={<Link to="/" />}>Start a new idea</Button>
          </div>
        )}

        {!isLoading && !isError && projects && projects.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Latest version</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Created</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {projects.map((project) => (
                <TableRow
                  key={project.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/projects/${project.id}`)}
                >
                  <TableCell className="font-medium">
                    <span className="flex items-center gap-2">
                      <FolderOpen className="text-primary size-4 shrink-0" />
                      {project.name}
                    </span>
                  </TableCell>
                  <TableCell>v{project.latestVersionNumber}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">{statusLabel(project.latestStatus)}</Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {project.createdAt ? new Date(project.createdAt).toLocaleDateString() : '—'}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  )
}
