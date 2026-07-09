import { CheckCircle2, Circle, Lock } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { ProjectVersionResponse } from '@/api/generated'
import { Logo } from '@/components/Logo'
import { ModeToggle } from '@/components/mode-toggle'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@/components/ui/sidebar'
import { STAGES, stageState, type StageState } from '@/lib/pipeline'

const STATE_ICON: Record<StageState, typeof CheckCircle2> = {
  done: CheckCircle2,
  current: Circle,
  locked: Lock,
}

function scrollToStage(key: string) {
  document.getElementById(key)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

export function WorkspaceSidebar({ version }: { version: ProjectVersionResponse }) {
  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <Link to="/" className="flex items-center gap-2 px-2 py-1.5 text-sm font-semibold">
          <Logo className="size-6 shrink-0" />
          <span className="truncate group-data-[collapsible=icon]:hidden">BlueForge</span>
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Pipeline</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {STAGES.map((stage) => {
                const state = stageState(stage.key, version.status)
                const Icon = STATE_ICON[state]
                return (
                  <SidebarMenuItem key={stage.key}>
                    <SidebarMenuButton
                      onClick={() => scrollToStage(stage.key)}
                      disabled={state === 'locked'}
                      isActive={state === 'current'}
                      tooltip={stage.label}
                    >
                      <Icon className="size-4" />
                      <span>{stage.label}</span>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                )
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>
        <div className="flex items-center justify-between px-1 group-data-[collapsible=icon]:justify-center">
          <span className="text-muted-foreground text-xs group-data-[collapsible=icon]:hidden">Theme</span>
          <ModeToggle />
        </div>
      </SidebarFooter>
    </Sidebar>
  )
}
