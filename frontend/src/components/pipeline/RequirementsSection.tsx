import { useQueryClient } from '@tanstack/react-query'
import { ListChecks } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import {
  RegenerateVersionRequestTargetStage,
  getGetProjectQueryKey,
  getListProjectsQueryKey,
  useRegenerateVersion,
  useUpdateRequirement,
} from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Badge } from '@/components/ui/badge'
import { patchVersionArrayItem } from '@/lib/cache'
import { stageState } from '@/lib/pipeline'
import { EditItemDialog } from './EditItemDialog'
import { RegenerateDialog } from './RegenerateDialog'
import { StageSection } from './StageSection'

const TYPE_LABEL: Record<string, string> = {
  FUNCTIONAL: 'Functional',
  NON_FUNCTIONAL: 'Non-functional',
  CONSTRAINT: 'Constraint',
  ASSUMPTION: 'Assumption',
}

export function RequirementsSection({ version }: { version: ProjectVersionResponse }) {
  const requirements = version.requirements ?? []
  const state = stageState('requirements', version.status)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const updateRequirement = useUpdateRequirement()
  const regenerateVersion = useRegenerateVersion()

  function handleSave(requirementId: number, values: Record<string, string>, onSaved: () => void) {
    updateRequirement.mutate(
      { requirementId, data: { title: values.title, description: values.description } },
      {
        onSuccess: (updated) => {
          patchVersionArrayItem(queryClient, version.projectId!, version.versionNumber!, 'requirements', updated)
          onSaved()
        },
      },
    )
  }

  function handleRegenerate(changeDescription: string | undefined, onStarted: () => void) {
    regenerateVersion.mutate(
      {
        projectId: version.projectId!,
        versionNumber: version.versionNumber!,
        data: { targetStage: RegenerateVersionRequestTargetStage.REQUIREMENTS_GENERATED, changeDescription },
      },
      {
        onSuccess: (newVersion) => {
          queryClient.invalidateQueries({ queryKey: getGetProjectQueryKey(version.projectId!) })
          queryClient.invalidateQueries({ queryKey: getListProjectsQueryKey() })
          onStarted()
          navigate(`/projects/${newVersion.projectId}/versions/${newVersion.versionNumber}`)
        },
      },
    )
  }

  return (
    <StageSection
      id="requirements"
      title="Requirements"
      icon={ListChecks}
      state={state}
      lockedMessage="Submit answers to the clarifying questions to generate requirements."
      action={
        state === 'done' && (
          <RegenerateDialog
            stageLabel="Requirements"
            onRegenerate={handleRegenerate}
            isPending={regenerateVersion.isPending}
            isError={regenerateVersion.isError}
          />
        )
      }
    >
      <ul className="flex flex-col gap-3">
        {requirements.map((requirement) => (
          <li key={requirement.id} className="flex flex-col gap-1 rounded-md border p-3">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Badge variant="outline">{TYPE_LABEL[requirement.type ?? ''] ?? requirement.type}</Badge>
                <span className="font-medium">{requirement.title}</span>
              </div>
              <EditItemDialog
                itemLabel="requirement"
                fields={[
                  { key: 'title', label: 'Title' },
                  { key: 'description', label: 'Description', multiline: true },
                ]}
                values={{ title: requirement.title ?? '', description: requirement.description ?? '' }}
                onSave={(values, onSaved) => handleSave(requirement.id!, values, onSaved)}
                isPending={updateRequirement.isPending}
                isError={updateRequirement.isError}
              />
            </div>
            <p className="text-muted-foreground text-sm">{requirement.description}</p>
          </li>
        ))}
      </ul>
    </StageSection>
  )
}
