import { useQueryClient } from '@tanstack/react-query'
import { BookOpen, CheckSquare2, Layers } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { RegenerateVersionRequestTargetStage, getGetProjectQueryKey, useGenerateTasks, useRegenerateVersion, useUpdateTask } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { patchVersionArrayItem } from '@/lib/cache'
import { stageState } from '@/lib/pipeline'
import { EditItemDialog } from './EditItemDialog'
import { RegenerateDialog } from './RegenerateDialog'
import { StageSection } from './StageSection'

interface TasksSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

const PRIORITY_VARIANT: Record<string, 'default' | 'secondary' | 'outline'> = {
  HIGH: 'default',
  MEDIUM: 'secondary',
  LOW: 'outline',
}

export function TasksSection({ version, onUpdated }: TasksSectionProps) {
  const epics = version.epics ?? []
  const userStories = version.userStories ?? []
  const tasks = version.tasks ?? []
  const state = stageState('tasks', version.status)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const generateTasks = useGenerateTasks()
  const updateTask = useUpdateTask()
  const regenerateVersion = useRegenerateVersion()

  const epicIds = epics.map((epic) => String(epic.id))

  function handleGenerate() {
    generateTasks.mutate(
      { projectId: version.projectId!, versionNumber: version.versionNumber! },
      { onSuccess: onUpdated },
    )
  }

  function handleRegenerate(changeDescription: string | undefined, onStarted: () => void) {
    regenerateVersion.mutate(
      {
        projectId: version.projectId!,
        versionNumber: version.versionNumber!,
        data: { targetStage: RegenerateVersionRequestTargetStage.TASKS_GENERATED, changeDescription },
      },
      {
        onSuccess: (newVersion) => {
          queryClient.invalidateQueries({ queryKey: getGetProjectQueryKey(version.projectId!) })
          onStarted()
          navigate(`/projects/${newVersion.projectId}/versions/${newVersion.versionNumber}`)
        },
      },
    )
  }

  function handleSave(taskId: number, values: Record<string, string>, onSaved: () => void) {
    updateTask.mutate(
      { taskId, data: { title: values.title, description: values.description } },
      {
        onSuccess: (updated) => {
          patchVersionArrayItem(queryClient, version.projectId!, version.versionNumber!, 'tasks', updated)
          onSaved()
        },
      },
    )
  }

  return (
    <StageSection
      id="tasks"
      title="Tasks"
      icon={CheckSquare2}
      state={state}
      lockedMessage="Generate user stories to unlock task generation."
      action={
        state === 'current' ? (
          <Button onClick={handleGenerate} disabled={generateTasks.isPending} className="self-start">
            {generateTasks.isPending ? 'Generating Tasks...' : 'Generate Tasks'}
          </Button>
        ) : (
          state === 'done' && (
            <RegenerateDialog
              stageLabel="Tasks"
              onRegenerate={handleRegenerate}
              isPending={regenerateVersion.isPending}
              isError={regenerateVersion.isError}
            />
          )
        )
      }
    >
      <Accordion defaultValue={epicIds}>
        {epics.map((epic) => {
          const stories = userStories.filter((story) => story.epicId === epic.id)
          const epicTaskCount = tasks.filter((task) =>
            stories.some((story) => story.id === task.userStoryId),
          ).length
          if (stories.length === 0) return null

          const storyIds = stories.map((story) => String(story.id))

          return (
            <AccordionItem key={epic.id} value={String(epic.id)}>
              <AccordionTrigger>
                <span className="flex items-center gap-2">
                  <Layers className="text-primary size-4" />
                  {epic.title}
                  <Badge variant="secondary">{epicTaskCount} tasks</Badge>
                </span>
              </AccordionTrigger>
              <AccordionContent>
                <Accordion defaultValue={storyIds} className="pl-6">
                  {stories.map((story) => {
                    const storyTasks = tasks.filter((task) => task.userStoryId === story.id)
                    if (storyTasks.length === 0) return null

                    return (
                      <AccordionItem key={story.id} value={String(story.id)}>
                        <AccordionTrigger>
                          <span className="flex items-center gap-2">
                            <BookOpen className="text-muted-foreground size-3.5" />
                            {story.title}
                            <Badge variant="outline">{storyTasks.length}</Badge>
                          </span>
                        </AccordionTrigger>
                        <AccordionContent>
                          <ul className="flex flex-col gap-2 pl-6">
                            {storyTasks.map((task) => (
                              <li key={task.id} className="flex flex-col gap-1 rounded-md border p-3">
                                <div className="flex flex-wrap items-center gap-2">
                                  <CheckSquare2 className="text-muted-foreground size-4 shrink-0" />
                                  <span className="font-medium">{task.title}</span>
                                  {task.priority && (
                                    <Badge variant={PRIORITY_VARIANT[task.priority] ?? 'outline'}>{task.priority}</Badge>
                                  )}
                                  {task.effortEstimate && <Badge variant="outline">Effort: {task.effortEstimate}</Badge>}
                                  <EditItemDialog
                                    itemLabel="task"
                                    fields={[
                                      { key: 'title', label: 'Title' },
                                      { key: 'description', label: 'Description', multiline: true },
                                    ]}
                                    values={{ title: task.title ?? '', description: task.description ?? '' }}
                                    onSave={(values, onSaved) => handleSave(task.id!, values, onSaved)}
                                    isPending={updateTask.isPending}
                                    isError={updateTask.isError}
                                  />
                                </div>
                                <p className="text-muted-foreground text-sm">{task.description}</p>
                              </li>
                            ))}
                          </ul>
                        </AccordionContent>
                      </AccordionItem>
                    )
                  })}
                </Accordion>
              </AccordionContent>
            </AccordionItem>
          )
        })}
      </Accordion>
      {generateTasks.isError && <p className="text-destructive text-sm">Failed to generate tasks. Please try again.</p>}
    </StageSection>
  )
}
