import { Clock, FolderOpen, Lightbulb, Loader2, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCreateProject } from '@/api/generated'
import { Logo } from '@/components/Logo'
import { ModeToggle } from '@/components/mode-toggle'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { getRecentProjects, recordRecentProject } from '@/lib/recent-projects'

const NEW_PROJECT_VERSION_NUMBER = 1

export function NewIdeaPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [ideaDescription, setIdeaDescription] = useState('')
  const createProject = useCreateProject()
  const recentProjects = getRecentProjects()

  const canSubmit = name.trim().length > 0 && ideaDescription.trim().length > 0

  function handleSubmit() {
    createProject.mutate(
      { data: { name, ideaDescription } },
      {
        onSuccess: (response) => {
          recordRecentProject({ projectId: response.projectId!, versionNumber: NEW_PROJECT_VERSION_NUMBER, name })
          navigate(`/projects/${response.projectId}/versions/${NEW_PROJECT_VERSION_NUMBER}`)
        },
      },
    )
  }

  return (
    <div className="from-background to-accent/10 min-h-svh bg-gradient-to-b">
      <div className="flex justify-end gap-2 p-4">
        <Button render={<Link to="/projects" />} variant="outline" className="gap-1.5">
          <FolderOpen className="size-4" />
          All projects
        </Button>
        <ModeToggle />
      </div>
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-10 p-8 pt-4">
        <div className="flex flex-col items-center gap-3 text-center">
          <Logo className="size-11" />
          <div className="flex flex-col gap-1.5">
            <h1 className="text-2xl font-semibold tracking-tight">BlueForge</h1>
            <p className="text-muted-foreground">Turn a software idea into requirements, epics, user stories, and tasks.</p>
          </div>
        </div>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Lightbulb className="text-primary size-5" />
              Start a new idea
            </CardTitle>
            <CardDescription>Describe what you want to build. BlueForge will ask clarifying questions next.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <Label htmlFor="project-name">Project name</Label>
              <Input id="project-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Recipe Sharing App" />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="idea-description">Idea description</Label>
              <Textarea
                id="idea-description"
                value={ideaDescription}
                onChange={(e) => setIdeaDescription(e.target.value)}
                placeholder="Describe the problem, the users, and what the app should do..."
                rows={6}
              />
            </div>
            <Button onClick={handleSubmit} disabled={!canSubmit || createProject.isPending} className="gap-1.5 self-start">
              {createProject.isPending ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <Sparkles className="size-4" />
                  Start planning
                </>
              )}
            </Button>
            {createProject.isError && (
              <p className="text-destructive text-sm">Failed to create the project. Please try again.</p>
            )}
          </CardContent>
        </Card>

        {recentProjects.length > 0 && (
          <div className="flex flex-col gap-3">
            <h2 className="text-muted-foreground flex items-center gap-1.5 text-sm font-medium">
              <Clock className="size-3.5" />
              Recent projects
            </h2>
            <div className="flex flex-col gap-2">
              {recentProjects.map((project) => (
                <button
                  key={`${project.projectId}-${project.versionNumber}`}
                  onClick={() => navigate(`/projects/${project.projectId}/versions/${project.versionNumber}`)}
                  className="hover:border-primary/40 hover:bg-accent/50 flex items-center gap-3 rounded-lg border bg-card px-4 py-3 text-left text-sm shadow-sm transition-colors"
                >
                  <FolderOpen className="text-primary size-4 shrink-0" />
                  <span className="flex-1 truncate font-medium">{project.name}</span>
                  <span className="text-muted-foreground text-xs">
                    Project {project.projectId} · v{project.versionNumber}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
