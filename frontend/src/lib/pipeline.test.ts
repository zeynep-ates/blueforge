import { describe, expect, it } from 'vitest'
import { ProjectVersionResponseStatus } from '@/api/generated'
import { currentStage, stageState } from './pipeline'

describe('stageState', () => {
  it('marks questions as current and everything else locked when awaiting answers', () => {
    const status = ProjectVersionResponseStatus.AWAITING_ANSWERS
    expect(stageState('questions', status)).toBe('current')
    expect(stageState('requirements', status)).toBe('locked')
    expect(stageState('epics', status)).toBe('locked')
    expect(stageState('userStories', status)).toBe('locked')
    expect(stageState('tasks', status)).toBe('locked')
  })

  it('unlocks epics and marks requirements done once requirements are generated', () => {
    const status = ProjectVersionResponseStatus.REQUIREMENTS_GENERATED
    expect(stageState('questions', status)).toBe('done')
    expect(stageState('requirements', status)).toBe('done')
    expect(stageState('epics', status)).toBe('current')
    expect(stageState('userStories', status)).toBe('locked')
    expect(stageState('tasks', status)).toBe('locked')
  })

  it('unlocks user stories once epics are generated', () => {
    const status = ProjectVersionResponseStatus.EPICS_GENERATED
    expect(stageState('epics', status)).toBe('done')
    expect(stageState('userStories', status)).toBe('current')
    expect(stageState('tasks', status)).toBe('locked')
  })

  it('unlocks tasks once user stories are generated', () => {
    const status = ProjectVersionResponseStatus.USER_STORIES_GENERATED
    expect(stageState('userStories', status)).toBe('done')
    expect(stageState('tasks', status)).toBe('current')
  })

  it('unlocks architecture once tasks are generated', () => {
    const status = ProjectVersionResponseStatus.TASKS_GENERATED
    expect(stageState('questions', status)).toBe('done')
    expect(stageState('requirements', status)).toBe('done')
    expect(stageState('epics', status)).toBe('done')
    expect(stageState('userStories', status)).toBe('done')
    expect(stageState('tasks', status)).toBe('done')
    expect(stageState('architecture', status)).toBe('current')
  })

  it('marks every stage done once architecture recommendations are generated', () => {
    const status = ProjectVersionResponseStatus.ARCHITECTURE_GENERATED
    expect(stageState('tasks', status)).toBe('done')
    expect(stageState('architecture', status)).toBe('done')
  })

  it('treats an undefined status the same as awaiting answers', () => {
    expect(stageState('questions', undefined)).toBe('current')
    expect(stageState('epics', undefined)).toBe('locked')
  })
})

describe('currentStage', () => {
  it('returns the stage awaiting the next action', () => {
    expect(currentStage(ProjectVersionResponseStatus.AWAITING_ANSWERS)).toBe('questions')
    expect(currentStage(ProjectVersionResponseStatus.REQUIREMENTS_GENERATED)).toBe('epics')
    expect(currentStage(ProjectVersionResponseStatus.EPICS_GENERATED)).toBe('userStories')
    expect(currentStage(ProjectVersionResponseStatus.USER_STORIES_GENERATED)).toBe('tasks')
    expect(currentStage(ProjectVersionResponseStatus.TASKS_GENERATED)).toBe('architecture')
  })

  it('falls back to architecture once the pipeline is fully generated', () => {
    expect(currentStage(ProjectVersionResponseStatus.ARCHITECTURE_GENERATED)).toBe('architecture')
  })
})
