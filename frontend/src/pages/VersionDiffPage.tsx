import {
  ArrowLeft,
  BookOpen,
  Building2,
  CheckSquare2,
  FileQuestion,
  GitCompare,
  Layers,
  ListChecks,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import {
  useGetVersionDiff,
  type ArchitectureRecommendationDiffEntry,
  type EpicDiffEntry,
  type RequirementDiffEntry,
  type TaskDiffEntry,
  type UserStoryDiffEntry,
} from '@/api/generated'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

type ChangeType = 'ADDED' | 'REMOVED' | 'MODIFIED' | 'UNCHANGED'

const CHANGE_STYLES: Record<ChangeType, { badge: string; border: string; label: string }> = {
  ADDED: { badge: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400', border: 'border-l-emerald-500', label: 'Added' },
  REMOVED: { badge: 'bg-destructive/10 text-destructive', border: 'border-l-destructive', label: 'Removed' },
  MODIFIED: { badge: 'bg-amber-500/10 text-amber-700 dark:text-amber-400', border: 'border-l-amber-500', label: 'Modified' },
  UNCHANGED: { badge: 'bg-transparent text-muted-foreground', border: 'border-l-transparent', label: 'Unchanged' },
}

function ChangeBadge({ changeType }: { changeType?: ChangeType }) {
  const style = CHANGE_STYLES[changeType ?? 'UNCHANGED']
  return (
    <Badge variant="outline" className={cn('border-transparent', style.badge)}>
      {style.label}
    </Badge>
  )
}

function DiffField({ label, before, after }: { label: string; before?: string | null; after?: string | null }) {
  if (before === after) {
    return (
      <p className="text-muted-foreground text-sm">
        {label && <span className="font-medium">{label}: </span>}
        {after ?? before}
      </p>
    )
  }
  return (
    <div className="text-sm">
      {before != null && (
        <p className="text-destructive line-through decoration-destructive/50">
          {label && <span className="font-medium">{label}: </span>}
          {before}
        </p>
      )}
      {after != null && (
        <p className="text-emerald-700 dark:text-emerald-400">
          {label && <span className="font-medium">{label}: </span>}
          {after}
        </p>
      )}
    </div>
  )
}

function RequirementRow({ entry }: { entry: RequirementDiffEntry }) {
  const item = entry.after ?? entry.before
  const style = CHANGE_STYLES[(entry.changeType as ChangeType) ?? 'UNCHANGED']
  return (
    <li className={cn('flex flex-col gap-1 rounded-md border border-l-4 p-3', style.border)}>
      <div className="flex items-center gap-2">
        <Badge variant="outline">{item?.type}</Badge>
        <DiffField label="" before={entry.before?.title} after={entry.after?.title} />
        <ChangeBadge changeType={entry.changeType as ChangeType} />
      </div>
      <DiffField label="" before={entry.before?.description} after={entry.after?.description} />
    </li>
  )
}

function ArchitectureRecommendationRow({ entry }: { entry: ArchitectureRecommendationDiffEntry }) {
  const style = CHANGE_STYLES[(entry.changeType as ChangeType) ?? 'UNCHANGED']
  return (
    <li className={cn('flex flex-col gap-1 rounded-md border border-l-4 p-3', style.border)}>
      <div className="flex items-center gap-2">
        <Building2 className="text-primary size-4 shrink-0" />
        <DiffField label="" before={entry.before?.component} after={entry.after?.component} />
        <ChangeBadge changeType={entry.changeType as ChangeType} />
      </div>
      <DiffField label="" before={entry.before?.recommendation} after={entry.after?.recommendation} />
      <DiffField label="Reasoning" before={entry.before?.reasoning} after={entry.after?.reasoning} />
      <DiffField label="Trade-offs" before={entry.before?.tradeoffs} after={entry.after?.tradeoffs} />
    </li>
  )
}

function TaskRow({ entry }: { entry: TaskDiffEntry }) {
  const item = entry.after ?? entry.before
  const style = CHANGE_STYLES[(entry.changeType as ChangeType) ?? 'UNCHANGED']
  return (
    <li className={cn('flex flex-col gap-1 rounded-md border border-l-4 p-3', style.border)}>
      <div className="flex flex-wrap items-center gap-2">
        <CheckSquare2 className="text-muted-foreground size-4 shrink-0" />
        <DiffField label="" before={entry.before?.title} after={entry.after?.title} />
        {item?.priority && <Badge variant="outline">{item.priority}</Badge>}
        {item?.effortEstimate && <Badge variant="outline">Effort: {item.effortEstimate}</Badge>}
        <ChangeBadge changeType={entry.changeType as ChangeType} />
      </div>
      <DiffField label="" before={entry.before?.description} after={entry.after?.description} />
    </li>
  )
}

function UserStoryBlock({ entry }: { entry: UserStoryDiffEntry }) {
  const item = entry.after ?? entry.before
  const tasks = entry.tasks ?? []
  return (
    <AccordionItem value={String(item?.id ?? item?.title)}>
      <AccordionTrigger>
        <span className="flex items-center gap-2">
          <BookOpen className="text-muted-foreground size-3.5" />
          {item?.title}
          <ChangeBadge changeType={entry.changeType as ChangeType} />
          <Badge variant="secondary">{tasks.length} tasks</Badge>
        </span>
      </AccordionTrigger>
      <AccordionContent>
        <div className="mb-2 pl-6">
          <DiffField label="" before={entry.before?.description} after={entry.after?.description} />
          <DiffField
            label="Acceptance criteria"
            before={entry.before?.acceptanceCriteria}
            after={entry.after?.acceptanceCriteria}
          />
        </div>
        <ul className="flex flex-col gap-2 pl-6">
          {tasks.map((task, i) => (
            <TaskRow key={task.after?.id ?? task.before?.id ?? i} entry={task} />
          ))}
        </ul>
      </AccordionContent>
    </AccordionItem>
  )
}

function EpicBlock({ entry }: { entry: EpicDiffEntry }) {
  const item = entry.after ?? entry.before
  const userStories = entry.userStories ?? []
  const storyIds = userStories.map((s, i) => String((s.after ?? s.before)?.id ?? i))

  return (
    <AccordionItem value={String(item?.id ?? item?.title)}>
      <AccordionTrigger>
        <span className="flex items-center gap-2">
          <Layers className="text-primary size-4" />
          {item?.title}
          <ChangeBadge changeType={entry.changeType as ChangeType} />
          <Badge variant="secondary">{userStories.length} stories</Badge>
        </span>
      </AccordionTrigger>
      <AccordionContent>
        <div className="mb-2 pl-6">
          <DiffField label="" before={entry.before?.description} after={entry.after?.description} />
        </div>
        <Accordion defaultValue={storyIds} className="pl-6">
          {userStories.map((story, i) => (
            <UserStoryBlock key={story.after?.id ?? story.before?.id ?? i} entry={story} />
          ))}
        </Accordion>
      </AccordionContent>
    </AccordionItem>
  )
}

export function VersionDiffPage() {
  const { projectId, fromVersion, toVersion } = useParams<{
    projectId: string
    fromVersion: string
    toVersion: string
  }>()
  const projectIdNum = Number(projectId)
  const fromVersionNum = Number(fromVersion)
  const toVersionNum = Number(toVersion)

  const { data: diff, isLoading, isError } = useGetVersionDiff(projectIdNum, fromVersionNum, toVersionNum)

  if (isLoading) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 p-8">
        <Skeleton className="h-8 w-64" />
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full" />
        ))}
      </div>
    )
  }

  if (isError || !diff) {
    return (
      <div className="flex min-h-svh flex-col items-center justify-center gap-4 p-16 text-center">
        <FileQuestion className="text-muted-foreground size-10" />
        <p className="text-muted-foreground max-w-sm">
          Couldn't compare those versions. One of them may not exist, or the backend may be unreachable.
        </p>
        <Button render={<Link to={`/projects/${projectIdNum}`} />}>Back to project</Button>
      </div>
    )
  }

  const epicIds = (diff.epics ?? []).map((epic, i) => String((epic.after ?? epic.before)?.id ?? i))

  return (
    <div className="from-background to-accent/10 min-h-svh bg-gradient-to-b">
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 p-8">
        <Button render={<Link to={`/projects/${projectIdNum}`} />} variant="ghost" className="w-fit gap-1.5 px-2">
          <ArrowLeft className="size-4" />
          Back to project
        </Button>

        <div className="flex flex-col gap-1.5">
          <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight">
            <GitCompare className="text-primary size-6" />
            Version {diff.fromVersionNumber} → Version {diff.toVersionNumber}
          </h1>
          <div className="flex flex-wrap gap-2">
            <Badge variant="outline" className={cn('border-transparent', CHANGE_STYLES.ADDED.badge)}>
              {diff.summary?.addedCount ?? 0} added
            </Badge>
            <Badge variant="outline" className={cn('border-transparent', CHANGE_STYLES.REMOVED.badge)}>
              {diff.summary?.removedCount ?? 0} removed
            </Badge>
            <Badge variant="outline" className={cn('border-transparent', CHANGE_STYLES.MODIFIED.badge)}>
              {diff.summary?.modifiedCount ?? 0} modified
            </Badge>
            <Badge variant="secondary">{diff.summary?.unchangedCount ?? 0} unchanged</Badge>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <ListChecks className="text-primary size-5" />
              Requirements
            </CardTitle>
          </CardHeader>
          <CardContent>
            {(diff.requirements ?? []).length === 0 ? (
              <p className="text-muted-foreground text-sm">No requirements in either version.</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {(diff.requirements ?? []).map((entry, i) => (
                  <RequirementRow key={entry.after?.id ?? entry.before?.id ?? i} entry={entry} />
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Layers className="text-primary size-5" />
              Epics
            </CardTitle>
          </CardHeader>
          <CardContent>
            {(diff.epics ?? []).length === 0 ? (
              <p className="text-muted-foreground text-sm">No epics in either version.</p>
            ) : (
              <Accordion defaultValue={epicIds}>
                {(diff.epics ?? []).map((entry, i) => (
                  <EpicBlock key={entry.after?.id ?? entry.before?.id ?? i} entry={entry} />
                ))}
              </Accordion>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Building2 className="text-primary size-5" />
              Architecture Recommendations
            </CardTitle>
          </CardHeader>
          <CardContent>
            {(diff.architectureRecommendations ?? []).length === 0 ? (
              <p className="text-muted-foreground text-sm">No architecture recommendations in either version.</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {(diff.architectureRecommendations ?? []).map((entry, i) => (
                  <ArchitectureRecommendationRow key={entry.after?.id ?? entry.before?.id ?? i} entry={entry} />
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
