import { useQueryClient } from '@tanstack/react-query'
import { BookOpen } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { RegenerateVersionRequestTargetStage, getGetProjectQueryKey, useGenerateUserStories, useRegenerateVersion, useUpdateUserStory } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { patchVersionArrayItem } from '@/lib/cache'
import { stageState } from '@/lib/pipeline'
import { EditItemDialog } from './EditItemDialog'
import { RegenerateDialog } from './RegenerateDialog'
import { StageSection } from './StageSection'

interface UserStoriesSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

export function UserStoriesSection({ version, onUpdated }: UserStoriesSectionProps) {
  const epics = version.epics ?? []
  const userStories = version.userStories ?? []
  const state = stageState('userStories', version.status)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const generateUserStories = useGenerateUserStories()
  const updateUserStory = useUpdateUserStory()
  const regenerateVersion = useRegenerateVersion()

  const epicIds = epics.map((epic) => String(epic.id))

  function handleGenerate() {
    generateUserStories.mutate(
      { projectId: version.projectId!, versionNumber: version.versionNumber! },
      { onSuccess: onUpdated },
    )
  }

  function handleRegenerate(changeDescription: string | undefined, onStarted: () => void) {
    regenerateVersion.mutate(
      {
        projectId: version.projectId!,
        versionNumber: version.versionNumber!,
        data: { targetStage: RegenerateVersionRequestTargetStage.USER_STORIES_GENERATED, changeDescription },
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

  function handleSave(userStoryId: number, values: Record<string, string>, onSaved: () => void) {
    updateUserStory.mutate(
      {
        userStoryId,
        data: {
          title: values.title,
          description: values.description,
          acceptanceCriteria: values.acceptanceCriteria,
        },
      },
      {
        onSuccess: (updated) => {
          patchVersionArrayItem(queryClient, version.projectId!, version.versionNumber!, 'userStories', updated)
          onSaved()
        },
      },
    )
  }

  return (
    <StageSection
      id="userStories"
      title="User Stories"
      icon={BookOpen}
      state={state}
      lockedMessage="Generate epics to unlock user story generation."
      action={
        state === 'current' ? (
          <Button onClick={handleGenerate} disabled={generateUserStories.isPending} className="self-start">
            {generateUserStories.isPending ? 'Generating User Stories...' : 'Generate User Stories'}
          </Button>
        ) : (
          state === 'done' && (
            <RegenerateDialog
              stageLabel="User Stories"
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
          if (stories.length === 0) return null

          return (
            <AccordionItem key={epic.id} value={String(epic.id)}>
              <AccordionTrigger>
                <span className="flex items-center gap-2">
                  <BookOpen className="text-primary size-4" />
                  {epic.title}
                  <Badge variant="secondary">{stories.length}</Badge>
                </span>
              </AccordionTrigger>
              <AccordionContent>
                <ul className="flex flex-col gap-2 pl-6">
                  {stories.map((story) => (
                    <li key={story.id} className="flex flex-col gap-1 rounded-md border p-3">
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-medium">{story.title}</span>
                        <EditItemDialog
                          itemLabel="user story"
                          fields={[
                            { key: 'title', label: 'Title' },
                            { key: 'description', label: 'Description', multiline: true },
                            { key: 'acceptanceCriteria', label: 'Acceptance criteria', multiline: true },
                          ]}
                          values={{
                            title: story.title ?? '',
                            description: story.description ?? '',
                            acceptanceCriteria: story.acceptanceCriteria ?? '',
                          }}
                          onSave={(values, onSaved) => handleSave(story.id!, values, onSaved)}
                          isPending={updateUserStory.isPending}
                          isError={updateUserStory.isError}
                        />
                      </div>
                      <p className="text-muted-foreground text-sm">{story.description}</p>
                      {story.acceptanceCriteria && (
                        <p className="text-muted-foreground text-xs">
                          <span className="font-medium">Acceptance criteria: </span>
                          {story.acceptanceCriteria}
                        </p>
                      )}
                    </li>
                  ))}
                </ul>
              </AccordionContent>
            </AccordionItem>
          )
        })}
      </Accordion>
      {generateUserStories.isError && (
        <p className="text-destructive text-sm">Failed to generate user stories. Please try again.</p>
      )}
    </StageSection>
  )
}
