import { CheckCircle2, Circle, Lock } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Badge } from '@/components/ui/badge'
import { Card, CardAction, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { StageState } from '@/lib/pipeline'

const STATE_BADGE: Record<StageState, { label: string; icon: ReactNode; variant: 'default' | 'secondary' | 'outline' }> = {
  done: { label: 'Done', icon: <CheckCircle2 className="size-3.5" />, variant: 'secondary' },
  current: { label: 'In progress', icon: <Circle className="size-3.5" />, variant: 'default' },
  locked: { label: 'Locked', icon: <Lock className="size-3.5" />, variant: 'outline' },
}

interface StageSectionProps {
  id: string
  title: string
  icon?: LucideIcon
  state: StageState
  action?: ReactNode
  children?: ReactNode
  lockedMessage?: string
}

export function StageSection({ id, title, icon: Icon, state, action, children, lockedMessage }: StageSectionProps) {
  const badge = STATE_BADGE[state]

  return (
    <Card id={id} className="scroll-mt-6 transition-shadow hover:shadow-md">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-lg">
          {Icon && <Icon className="text-primary size-5" />}
          {title}
        </CardTitle>
        <CardAction>
          <Badge variant={badge.variant} className="gap-1">
            {badge.icon}
            {badge.label}
          </Badge>
        </CardAction>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {state === 'locked' ? (
          <p className="text-muted-foreground text-sm">{lockedMessage ?? 'Complete the previous stage to unlock this.'}</p>
        ) : (
          children
        )}
        {state !== 'locked' && action}
      </CardContent>
    </Card>
  )
}
