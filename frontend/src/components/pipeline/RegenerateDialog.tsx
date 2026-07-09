import { Loader2, RotateCcw } from 'lucide-react'
import { useId, useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'

interface RegenerateDialogProps {
  stageLabel: string
  onRegenerate: (changeDescription: string | undefined, onStarted: () => void) => void
  isPending: boolean
  isError: boolean
}

export function RegenerateDialog({ stageLabel, onRegenerate, isPending, isError }: RegenerateDialogProps) {
  const instanceId = useId()
  const [open, setOpen] = useState(false)
  const [note, setNote] = useState('')

  function handleConfirm() {
    onRegenerate(note.trim() || undefined, () => setOpen(false))
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button variant="outline" size="sm" className="gap-1.5 self-start" />}>
        <RotateCcw className="size-3.5" />
        Regenerate {stageLabel}
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Regenerate {stageLabel}</DialogTitle>
          <DialogDescription>
            This creates a new project version, regenerated from {stageLabel.toLowerCase()} onward. The current
            version is left exactly as it is.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-2">
          <Label htmlFor={`${instanceId}-note`}>Note (optional)</Label>
          <Textarea
            id={`${instanceId}-note`}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            rows={3}
            placeholder="e.g. Tried a different angle"
          />
        </div>
        {isError && <p className="text-destructive text-sm">Failed to regenerate. Please try again.</p>}
        <DialogFooter>
          <DialogClose render={<Button variant="outline" />}>Cancel</DialogClose>
          <Button onClick={handleConfirm} disabled={isPending} className="gap-1.5">
            {isPending ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Regenerating...
              </>
            ) : (
              'Regenerate'
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
