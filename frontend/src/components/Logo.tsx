import { cn } from '@/lib/utils'

interface LogoProps {
  className?: string
}

/**
 * Two angle brackets (< >) rotated toward each other into a faceted diamond,
 * with a deliberate seam at the top and bottom where they don't quite meet -
 * code brackets forged into one cut shape. No container, no literal icon.
 */
export function Logo({ className }: LogoProps) {
  return (
    <svg viewBox="0 0 40 40" fill="none" className={cn('text-primary size-7', className)} aria-hidden="true">
      <path
        d="M4 20 L19 8.5 L19 14 L10 20 L19 26 L19 31.5 Z M36 20 L21 8.5 L21 14 L30 20 L21 26 L21 31.5 Z"
        fill="currentColor"
      />
    </svg>
  )
}
