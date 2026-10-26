import {
  useState,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
} from 'react'
import { Icon, type IconName } from './icons.tsx'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'ghost' | 'danger' | 'text'
}

export function Button({ variant = 'primary', className, ...props }: ButtonProps) {
  return <button className={`btn btn--${variant}${className ? ` ${className}` : ''}`} {...props} />
}

// Visual "*" after a label. Inputs carry `required` for assistive tech, so this stays hidden from it.
export function RequiredMark() {
  return (
    <span className="field__required" aria-hidden="true">
      *
    </span>
  )
}

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  hint?: string
  revealable?: boolean
  revealShowLabel?: string
  revealHideLabel?: string
}

export function Field({
  label,
  hint,
  id,
  type,
  revealable = false,
  revealShowLabel = 'Show',
  revealHideLabel = 'Hide',
  ...props
}: FieldProps) {
  const fieldId = id ?? props.name
  const [revealed, setRevealed] = useState(false)
  const inputType = revealable ? (revealed ? 'text' : 'password') : type
  return (
    <label className="field" htmlFor={fieldId}>
      <span className="field__label">
        {label}
        {props.required ? <RequiredMark /> : null}
      </span>
      <span className={`field__input${revealable ? ' field__input--reveal' : ''}`}>
        <input id={fieldId} className="field__control" type={inputType} {...props} />
        {revealable ? (
          <button
            type="button"
            className="field__reveal"
            aria-label={revealed ? revealHideLabel : revealShowLabel}
            aria-pressed={revealed}
            onClick={() => setRevealed((current) => !current)}
          >
            {revealed ? (
              <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M12 6c3.79 0 7.17 2.13 8.82 5.5a9.86 9.86 0 0 1-2.24 3.32l1.42 1.42c1.1-1.16 1.99-2.58 2.59-4.19C20.82 7.78 16.64 4.5 12 4.5c-1.27 0-2.49.23-3.64.65l1.62 1.62c.65-.17 1.33-.27 2.02-.27m-1.07 1.07L17.94 14.1c.5-.64.9-1.36 1.2-2.15C17.76 9.24 15.05 7.5 12 7.5c-.36 0-.71.03-1.07.07M2.1 4.93l1.56 1.56.36.36A11.8 11.8 0 0 0 1 11.5C2.73 15.89 7 19.5 12 19.5c1.73 0 3.36-.37 4.83-1.03l.4.4 2.54 2.54 1.27-1.27L3.37 3.66 2.1 4.93m7.54 7.54 1.11 1.11c-.22.07-.45.12-.7.12A2.5 2.5 0 0 1 7.55 11.2l1.11 1.11c-.01.05-.02.11-.02.16m3.73 3.73c-.7.17-1.42.27-2.17.27-3.05 0-5.76-1.74-7.14-4.27.49-.9 1.13-1.7 1.88-2.37l2.03 2.03A4 4 0 0 0 12 15.5c.32 0 .63-.04.93-.1z"
                />
              </svg>
            ) : (
              <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5M12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5m0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3"
                />
              </svg>
            )}
          </button>
        ) : null}
      </span>
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

export function PersonHeader({
  initials,
  name,
  role,
  children,
}: {
  initials: string
  name: string
  role: string
  children?: ReactNode
}) {
  return (
    <section className="person-header">
      <div className="identity">
        <div className="avatar avatar--lg" aria-hidden="true">
          {initials}
        </div>
        <div>
          <h2 className="card__title">{name}</h2>
          <p className="muted">{role}</p>
          {children ? <div className="chips person-header__chips">{children}</div> : null}
        </div>
      </div>
    </section>
  )
}

export function Panel({
  title,
  icon,
  children,
}: {
  title: string
  icon?: IconName
  children?: ReactNode
}) {
  return (
    <section className="panel">
      <h2 className="panel__title">
        {icon ? <Icon name={icon} size={18} /> : null}
        {title}
      </h2>
      {children}
    </section>
  )
}

export type InfoRow = {
  icon?: IconName
  label: string
  value: string
}

export function InfoList({ rows }: { rows: InfoRow[] }) {
  return (
    <dl className="info-list">
      {rows.map((row) => (
        <div className="info-list__row" key={row.label}>
          {row.icon ? (
            <span className="info-list__icon">
              <Icon name={row.icon} size={18} />
            </span>
          ) : null}
          <div>
            <dt className="info-list__label">{row.label}</dt>
            <dd className="info-list__value">{row.value}</dd>
          </div>
        </div>
      ))}
    </dl>
  )
}
