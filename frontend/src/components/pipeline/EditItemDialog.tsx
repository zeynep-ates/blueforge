import { Loader2, Pencil } from 'lucide-react'
import { useEffect, useId, useState } from 'react'
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
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'

export interface EditField {
  key: string
  label: string
  multiline?: boolean
}

interface EditItemDialogProps {
  itemLabel: string
  fields: EditField[]
  values: Record<string, string>
  onSave: (values: Record<string, string>, onSaved: () => void) => void
  isPending: boolean
  isError: boolean
}

export function EditItemDialog({ itemLabel, fields, values, onSave, isPending, isError }: EditItemDialogProps) {
  const instanceId = useId()
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState(values)

  useEffect(() => {
    if (open) setDraft(values)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const canSave = fields.every((field) => (draft[field.key] ?? '').trim().length > 0)

  function handleSave() {
    onSave(draft, () => setOpen(false))
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button variant="ghost" size="icon-sm" />}>
        <Pencil className="size-3.5" />
        <span className="sr-only">Edit {itemLabel}</span>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit {itemLabel}</DialogTitle>
          <DialogDescription>Update the text below, then save.</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          {fields.map((field) => (
            <div key={field.key} className="flex flex-col gap-2">
              <Label htmlFor={`${instanceId}-${field.key}`}>{field.label}</Label>
              {field.multiline ? (
                <Textarea
                  id={`${instanceId}-${field.key}`}
                  value={draft[field.key] ?? ''}
                  onChange={(e) => setDraft((d) => ({ ...d, [field.key]: e.target.value }))}
                  rows={4}
                />
              ) : (
                <Input
                  id={`${instanceId}-${field.key}`}
                  value={draft[field.key] ?? ''}
                  onChange={(e) => setDraft((d) => ({ ...d, [field.key]: e.target.value }))}
                />
              )}
            </div>
          ))}
          {isError && <p className="text-destructive text-sm">Failed to save changes. Please try again.</p>}
        </div>
        <DialogFooter>
          <DialogClose render={<Button variant="outline" />}>Cancel</DialogClose>
          <Button onClick={handleSave} disabled={!canSave || isPending} className="gap-1.5">
            {isPending ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Saving...
              </>
            ) : (
              'Save'
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
