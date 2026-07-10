import { useQueryClient } from '@tanstack/react-query'
import { Building2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import {
  RegenerateVersionRequestTargetStage,
  getGetProjectQueryKey,
  getListProjectsQueryKey,
  useGenerateArchitectureRecommendations,
  useRegenerateVersion,
} from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'
import { Button } from '@/components/ui/button'
import { stageState } from '@/lib/pipeline'
import { RegenerateDialog } from './RegenerateDialog'
import { StageSection } from './StageSection'

interface ArchitectureRecommendationsSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

export function ArchitectureRecommendationsSection({
  version,
  onUpdated,
}: ArchitectureRecommendationsSectionProps) {
  const recommendations = version.architectureRecommendations ?? []
  const state = stageState('architecture', version.status)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const generateArchitectureRecommendations = useGenerateArchitectureRecommendations()
  const regenerateVersion = useRegenerateVersion()

  function handleGenerate() {
    generateArchitectureRecommendations.mutate(
      { projectId: version.projectId!, versionNumber: version.versionNumber! },
      { onSuccess: onUpdated },
    )
  }

  function handleRegenerate(changeDescription: string | undefined, onStarted: () => void) {
    regenerateVersion.mutate(
      {
        projectId: version.projectId!,
        versionNumber: version.versionNumber!,
        data: { targetStage: RegenerateVersionRequestTargetStage.ARCHITECTURE_GENERATED, changeDescription },
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
      id="architecture"
      title="Architecture Recommendations"
      icon={Building2}
      state={state}
      lockedMessage="Generate tasks to unlock architecture recommendations."
      action={
        state === 'current' ? (
          <Button
            onClick={handleGenerate}
            disabled={generateArchitectureRecommendations.isPending}
            className="self-start"
          >
            {generateArchitectureRecommendations.isPending
              ? 'Generating Recommendations...'
              : 'Generate Architecture Recommendations'}
          </Button>
        ) : (
          state === 'done' && (
            <RegenerateDialog
              stageLabel="Architecture Recommendations"
              onRegenerate={handleRegenerate}
              isPending={regenerateVersion.isPending}
              isError={regenerateVersion.isError}
            />
          )
        )
      }
    >
      <ul className="flex flex-col gap-3">
        {recommendations.map((recommendation) => (
          <li key={recommendation.id} className="flex flex-col gap-1 rounded-md border p-3">
            <div className="flex items-center gap-2">
              <Building2 className="text-primary size-4" />
              <span className="font-medium">{recommendation.component}</span>
            </div>
            <p className="text-sm">{recommendation.recommendation}</p>
            <p className="text-muted-foreground text-sm">
              <span className="font-medium">Reasoning: </span>
              {recommendation.reasoning}
            </p>
            <p className="text-muted-foreground text-sm">
              <span className="font-medium">Trade-offs: </span>
              {recommendation.tradeoffs}
            </p>
          </li>
        ))}
      </ul>
      {generateArchitectureRecommendations.isError && (
        <p className="text-destructive text-sm">Failed to generate architecture recommendations. Please try again.</p>
      )}
    </StageSection>
  )
}
