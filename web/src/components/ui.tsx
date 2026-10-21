import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'ghost' | 'danger' | 'text'
}

export function Button({ variant = 'primary', className, ...props }: ButtonProps) {
  return <button className={`btn btn--${variant}${className ? ` ${className}` : ''}`} {...props} />
}

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  hint?: string
}

export function Field({ label, hint, id, ...props }: FieldProps) {
  const fieldId = id ?? props.name
  return (
    <label className="field" htmlFor={fieldId}>
      <span className="field__label">{label}</span>
      <input id={fieldId} className="field__control" {...props} />
      {hint ? <p className="field__hint">{hint}</p> : null}
    </label>
  )
}

type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string
  children: ReactNode
}

export function SelectField({ label, id, children, ...props }: SelectFieldProps) {
  const fieldId = id ?? props.name
  return (
    <label className="field" htmlFor={fieldId}>
      <span className="field__label">{label}</span>
      <select id={fieldId} className="field__control" {...props}>
        {children}
      </select>
    </label>
  )
}

type CardProps = {
  children?: ReactNode
  kicker?: string
  title?: string
}

export function Card({ children, kicker, title }: CardProps) {
  return (
    <section className="card">
      {kicker ? <p className="card__kicker">{kicker}</p> : null}
      {title ? <h2 className="card__title">{title}</h2> : null}
      {children}
    </section>
  )
}

type ChipProps = {
  label: string
  selected?: boolean
  onClick?: () => void
}

export function ChoiceChip({ label, selected = false, onClick }: ChipProps) {
  return (
    <button
      type="button"
      className={`chip${selected ? ' is-selected' : ''}`}
      aria-pressed={selected}
      onClick={onClick}
    >
      {label}
    </button>
  )
}

export function StatusChip({
  children,
  tone = 'green',
}: {
  children: ReactNode
  tone?: 'green' | 'saffron'
}) {
  return (
    <span className={`status-chip${tone === 'saffron' ? ' status-chip--saffron' : ''}`}>
      {children}
    </span>
  )
}

export function ErrorBanner({ message }: { message: string }) {
  if (!message) return null
  return (
    <div className="banner banner--error" role="alert">
      {message}
    </div>
  )
}

export function Screen({
  children,
  withNav = false,
}: {
  children: ReactNode
  withNav?: boolean
}) {
  return (
    <div className="screen">
      <div className={`screen-scroll${withNav ? ' screen-scroll--nav' : ''}`}>{children}</div>
    </div>
  )
}
