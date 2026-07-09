import { Loader2, MessageCircleQuestion } from 'lucide-react'
import { useState } from 'react'
import { useSubmitAnswers } from '@/api/generated'
import type { ClarifyingQuestionResponse, ProjectVersionResponse } from '@/api/generated'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { stageState } from '@/lib/pipeline'
import { StageSection } from './StageSection'

interface QuestionsSectionProps {
  version: ProjectVersionResponse
  onUpdated: (version: ProjectVersionResponse) => void
}

export function QuestionsSection({ version, onUpdated }: QuestionsSectionProps) {
  const questions = version.questions ?? []
  const state = stageState('questions', version.status)
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const submitAnswers = useSubmitAnswers()

  const isAnswered = state === 'done'
  const canSubmit = questions.every((q) => (answers[q.id!] ?? '').trim().length > 0)

  function handleSubmit() {
    submitAnswers.mutate(
      {
        projectId: version.projectId!,
        versionNumber: version.versionNumber!,
        data: {
          answers: questions.map((q) => ({ questionId: q.id!, answerText: answers[q.id!] })),
        },
      },
      { onSuccess: onUpdated },
    )
  }

  return (
    <StageSection id="questions" title="Clarifying Questions" icon={MessageCircleQuestion} state={state}>
      <div className="flex flex-col gap-5">
        {questions.map((question: ClarifyingQuestionResponse) => (
          <div key={question.id} className="flex flex-col gap-2">
            <Label htmlFor={`question-${question.id}`}>{question.questionText}</Label>
            {isAnswered ? (
              <p className="text-muted-foreground text-sm">{question.answerText}</p>
            ) : (
              <Textarea
                id={`question-${question.id}`}
                value={answers[question.id!] ?? ''}
                onChange={(e) => setAnswers((prev) => ({ ...prev, [question.id!]: e.target.value }))}
                placeholder="Your answer..."
              />
            )}
          </div>
        ))}
        {!isAnswered && (
          <Button onClick={handleSubmit} disabled={!canSubmit || submitAnswers.isPending} className="gap-1.5 self-start">
            {submitAnswers.isPending && <Loader2 className="size-4 animate-spin" />}
            {submitAnswers.isPending ? 'Submitting...' : 'Submit Answers'}
          </Button>
        )}
        {submitAnswers.isError && <p className="text-destructive text-sm">Failed to submit answers. Please try again.</p>}
      </div>
    </StageSection>
  )
}
