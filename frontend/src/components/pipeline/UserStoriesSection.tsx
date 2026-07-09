import { BookOpen } from 'lucide-react'
import { useGenerateUserStories } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { stageState } from '@/lib/pipeline'
import { StageSection } from './StageSection'

interface UserStoriesSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

export function UserStoriesSection({ version, onUpdated }: UserStoriesSectionProps) {
  const epics = version.epics ?? []
  const userStories = version.userStories ?? []
  const state = stageState('userStories', version.status)
  const generateUserStories = useGenerateUserStories()

  const epicIds = epics.map((epic) => String(epic.id))

  function handleGenerate() {
    generateUserStories.mutate(
      { projectId: version.projectId!, versionNumber: version.versionNumber! },
      { onSuccess: onUpdated },
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
        state === 'current' && (
          <Button onClick={handleGenerate} disabled={generateUserStories.isPending} className="self-start">
            {generateUserStories.isPending ? 'Generating User Stories...' : 'Generate User Stories'}
          </Button>
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
                      <span className="font-medium">{story.title}</span>
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
