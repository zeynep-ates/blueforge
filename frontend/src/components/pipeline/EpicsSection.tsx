import { useQueryClient } from '@tanstack/react-query'
import { Layers } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { RegenerateVersionRequestTargetStage, getGetProjectQueryKey, useGenerateEpics, useRegenerateVersion, useUpdateEpic } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Button } from '@/components/ui/button'
import { patchVersionArrayItem } from '@/lib/cache'
import { stageState } from '@/lib/pipeline'
import { EditItemDialog } from './EditItemDialog'
import { RegenerateDialog } from './RegenerateDialog'
import { StageSection } from './StageSection'

interface EpicsSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

export function EpicsSection({ version, onUpdated }: EpicsSectionProps) {
  const epics = version.epics ?? []
  const state = stageState('epics', version.status)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const generateEpics = useGenerateEpics()
  const updateEpic = useUpdateEpic()
  const regenerateVersion = useRegenerateVersion()

  function handleGenerate() {
    generateEpics.mutate(
      { projectId: version.projectId!, versionNumber: version.versionNumber! },
      { onSuccess: onUpdated },
    )
  }

  function handleRegenerate(changeDescription: string | undefined, onStarted: () => void) {
    regenerateVersion.mutate(
      {
        projectId: version.projectId!,
        versionNumber: version.versionNumber!,
        data: { targetStage: RegenerateVersionRequestTargetStage.EPICS_GENERATED, changeDescription },
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

  function handleSave(epicId: number, values: Record<string, string>, onSaved: () => void) {
    updateEpic.mutate(
      { epicId, data: { title: values.title, description: values.description } },
      {
        onSuccess: (updated) => {
          patchVersionArrayItem(queryClient, version.projectId!, version.versionNumber!, 'epics', updated)
          onSaved()
        },
      },
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
        state === 'current' ? (
          <Button onClick={handleGenerate} disabled={generateEpics.isPending} className="self-start">
            {generateEpics.isPending ? 'Generating Epics...' : 'Generate Epics'}
          </Button>
        ) : (
          state === 'done' && (
            <RegenerateDialog
              stageLabel="Epics"
              onRegenerate={handleRegenerate}
              isPending={regenerateVersion.isPending}
              isError={regenerateVersion.isError}
            />
          )
        )
      }
    >
      <ul className="flex flex-col gap-3">
        {epics.map((epic) => (
          <li key={epic.id} className="flex flex-col gap-1 rounded-md border p-3">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Layers className="text-primary size-4" />
                <span className="font-medium">{epic.title}</span>
              </div>
              <EditItemDialog
                itemLabel="epic"
                fields={[
                  { key: 'title', label: 'Title' },
                  { key: 'description', label: 'Description', multiline: true },
                ]}
                values={{ title: epic.title ?? '', description: epic.description ?? '' }}
                onSave={(values, onSaved) => handleSave(epic.id!, values, onSaved)}
                isPending={updateEpic.isPending}
                isError={updateEpic.isError}
              />
            </div>
            <p className="text-muted-foreground text-sm">{epic.description}</p>
          </li>
        ))}
      </ul>
      {generateEpics.isError && <p className="text-destructive text-sm">Failed to generate epics. Please try again.</p>}
    </StageSection>
  )
}
