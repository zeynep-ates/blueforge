import { useQueryClient } from '@tanstack/react-query'
import { ListChecks } from 'lucide-react'
import { useUpdateRequirement } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Badge } from '@/components/ui/badge'
import { patchVersionArrayItem } from '@/lib/cache'
import { stageState } from '@/lib/pipeline'
import { EditItemDialog } from './EditItemDialog'
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
  const updateRequirement = useUpdateRequirement()

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

  return (
    <StageSection
      id="requirements"
      title="Requirements"
      icon={ListChecks}
      state={state}
      lockedMessage="Submit answers to the clarifying questions to generate requirements."
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
