import { ListChecks } from 'lucide-react'
import type { ProjectVersionResponse } from '@/api/generated'
import { Badge } from '@/components/ui/badge'
import { stageState } from '@/lib/pipeline'
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
            <div className="flex items-center gap-2">
              <Badge variant="outline">{TYPE_LABEL[requirement.type ?? ''] ?? requirement.type}</Badge>
              <span className="font-medium">{requirement.title}</span>
            </div>
            <p className="text-muted-foreground text-sm">{requirement.description}</p>
          </li>
        ))}
      </ul>
    </StageSection>
  )
}
