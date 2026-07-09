import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EditItemDialog } from './EditItemDialog'

const fields = [
  { key: 'title', label: 'Title' },
  { key: 'description', label: 'Description', multiline: true },
]

const values = { title: 'Original title', description: 'Original description' }

describe('EditItemDialog', () => {
  it('opens with the current values pre-filled', () => {
    render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={vi.fn()} isPending={false} isError={false} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Edit requirement' }))

    expect(screen.getByLabelText('Title')).toHaveValue('Original title')
    expect(screen.getByLabelText('Description')).toHaveValue('Original description')
  })

  it('calls onSave with the edited values and closes on completion', () => {
    const onSave = vi.fn((_values, onSaved: () => void) => onSaved())
    render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={onSave} isPending={false} isError={false} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Edit requirement' }))
    fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'New title' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(onSave).toHaveBeenCalledWith({ title: 'New title', description: 'Original description' }, expect.any(Function))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('disables Save when a required field is cleared', () => {
    render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={vi.fn()} isPending={false} isError={false} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Edit requirement' }))
    fireEvent.change(screen.getByLabelText('Title'), { target: { value: '  ' } })

    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('shows an error message when isError is true', () => {
    render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={vi.fn()} isPending={false} isError={true} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Edit requirement' }))

    expect(screen.getByText('Failed to save changes. Please try again.')).toBeInTheDocument()
  })

  it('disables Save and shows a spinner label while pending', () => {
    render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={vi.fn()} isPending={true} isError={false} />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Edit requirement' }))

    expect(screen.getByRole('button', { name: 'Saving...' })).toBeDisabled()
  })

  it('uses unique field ids per instance so multiple dialogs never collide', () => {
    const { container: containerA } = render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={vi.fn()} isPending={false} isError={false} />,
    )
    const { container: containerB } = render(
      <EditItemDialog itemLabel="requirement" fields={fields} values={values} onSave={vi.fn()} isPending={false} isError={false} />,
    )

    fireEvent.click(containerA.querySelector('button')!)
    fireEvent.click(containerB.querySelector('button')!)

    const ids = screen.getAllByLabelText('Title').map((el) => el.id)
    expect(new Set(ids).size).toBe(ids.length)
  })
})
