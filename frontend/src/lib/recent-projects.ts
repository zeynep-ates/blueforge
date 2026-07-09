const STORAGE_KEY = 'blueforge.recentProjects'
const MAX_ENTRIES = 10

export interface RecentProject {
  projectId: number
  versionNumber: number
  name: string
  visitedAt: string
}

export function getRecentProjects(): RecentProject[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    return JSON.parse(raw) as RecentProject[]
  } catch {
    return []
  }
}

export function recordRecentProject(entry: Omit<RecentProject, 'visitedAt'>): void {
  const existing = getRecentProjects().filter(
    (project) => !(project.projectId === entry.projectId && project.versionNumber === entry.versionNumber),
  )
  const updated = [{ ...entry, visitedAt: new Date().toISOString() }, ...existing].slice(0, MAX_ENTRIES)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
}
