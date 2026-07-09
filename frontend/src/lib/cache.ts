import type { QueryClient } from '@tanstack/react-query'
import { getGetProjectVersionQueryKey } from '@/api/generated'
import type { ProjectVersionResponse } from '@/api/generated'

type VersionArrayKey = 'requirements' | 'epics' | 'userStories' | 'tasks'

/**
 * Edit endpoints return only the single updated entity (not the whole
 * ProjectVersionResponse), so mutations can't reuse the "overwrite the
 * cached blob" pattern used by the generation endpoints. This replaces just
 * the matching item inside the cached array instead, avoiding a refetch.
 */
export function patchVersionArrayItem<T extends { id?: number }>(
  queryClient: QueryClient,
  projectId: number,
  versionNumber: number,
  arrayKey: VersionArrayKey,
  updatedItem: T,
) {
  queryClient.setQueryData<ProjectVersionResponse>(getGetProjectVersionQueryKey(projectId, versionNumber), (current) => {
    if (!current) return current
    const items = current[arrayKey] as T[] | undefined
    if (!items) return current
    return {
      ...current,
      [arrayKey]: items.map((item) => (item.id === updatedItem.id ? updatedItem : item)),
    }
  })
}
