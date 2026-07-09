import { ProjectVersionResponseStatus, type ProjectVersionResponse } from '@/api/generated'

export type StageState = 'done' | 'current' | 'locked'

export type StageKey = 'questions' | 'requirements' | 'epics' | 'userStories' | 'tasks'

export interface StageDefinition {
  key: StageKey
  label: string
}

export const STAGES: StageDefinition[] = [
  { key: 'questions', label: 'Clarifying Questions' },
  { key: 'requirements', label: 'Requirements' },
  { key: 'epics', label: 'Epics' },
  { key: 'userStories', label: 'User Stories' },
  { key: 'tasks', label: 'Tasks' },
]

const STATUS_ORDER: ProjectVersionResponseStatus[] = [
  ProjectVersionResponseStatus.AWAITING_ANSWERS,
  ProjectVersionResponseStatus.REQUIREMENTS_GENERATED,
  ProjectVersionResponseStatus.EPICS_GENERATED,
  ProjectVersionResponseStatus.USER_STORIES_GENERATED,
  ProjectVersionResponseStatus.TASKS_GENERATED,
]

export function statusIndex(status: ProjectVersionResponseStatus | undefined): number {
  if (!status) return 0
  const idx = STATUS_ORDER.indexOf(status)
  return idx === -1 ? 0 : idx
}

/**
 * Requirements are generated as a byproduct of answering questions (no
 * standalone action), so they only ever transition locked -> done, never
 * 'current'.
 */
export function stageState(stage: StageKey, status: ProjectVersionResponseStatus | undefined): StageState {
  const idx = statusIndex(status)

  switch (stage) {
    case 'questions':
      return idx === 0 ? 'current' : 'done'
    case 'requirements':
      return idx === 0 ? 'locked' : 'done'
    case 'epics':
      if (idx < 1) return 'locked'
      return idx === 1 ? 'current' : 'done'
    case 'userStories':
      if (idx < 2) return 'locked'
      return idx === 2 ? 'current' : 'done'
    case 'tasks':
      if (idx < 3) return 'locked'
      return idx === 3 ? 'current' : 'done'
  }
}

export function currentStage(status: ProjectVersionResponseStatus | undefined): StageKey {
  const found = STAGES.find((stage) => stageState(stage.key, status) === 'current')
  return found?.key ?? 'tasks'
}

export function stageStatesFor(version: Pick<ProjectVersionResponse, 'status'>) {
  return Object.fromEntries(STAGES.map((stage) => [stage.key, stageState(stage.key, version.status)])) as Record<
    StageKey,
    StageState
  >
}
