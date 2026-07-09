import { Layers } from 'lucide-react'
import { useGenerateEpics } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Button } from '@/components/ui/button'
import { stageState } from '@/lib/pipeline'
import { StageSection } from './StageSection'

interface EpicsSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

export function EpicsSection({ version, onUpdated }: EpicsSectionProps) {
  const epics = version.epics ?? []
  const state = stageState('epics', version.status)
  const generateEpics = useGenerateEpics()

  function handleGenerate() {
    generateEpics.mutate(
      { projectId: version.projectId!, versionNumber: version.versionNumber! },
      { onSuccess: onUpdated },
    )
  }

  return (
    <StageSection
      id="epics"
      title="Epics"
      icon={Layers}
      state={state}
      lockedMessage="Answer the clarifying questions to unlock epic generation."
      action={
        state === 'current' && (
          <Button onClick={handleGenerate} disabled={generateEpics.isPending} className="self-start">
            {generateEpics.isPending ? 'Generating Epics...' : 'Generate Epics'}
          </Button>
        )
      }
    >
      <ul className="flex flex-col gap-3">
        {epics.map((epic) => (
          <li key={epic.id} className="flex flex-col gap-1 rounded-md border p-3">
            <div className="flex items-center gap-2">
              <Layers className="text-primary size-4" />
              <span className="font-medium">{epic.title}</span>
            </div>
            <p className="text-muted-foreground text-sm">{epic.description}</p>
          </li>
        ))}
      </ul>
      {generateEpics.isError && <p className="text-destructive text-sm">Failed to generate epics. Please try again.</p>}
    </StageSection>
  )
}
