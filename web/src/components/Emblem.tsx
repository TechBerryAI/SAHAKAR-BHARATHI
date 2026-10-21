type EmblemProps = {
  size?: number
}

export function Emblem({ size = 28 }: EmblemProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <circle cx="32" cy="32" r="30" fill="#0F3D2E" />
      <circle cx="32" cy="32" r="22" fill="none" stroke="#FE983A" strokeWidth="3" />
      <circle cx="32" cy="32" r="7" fill="#FFF8F3" />
      <g stroke="#FE983A" strokeWidth="2.4" strokeLinecap="round">
        <path d="M32 12v14M32 38v14M12 32h14M38 32h14M18.3 18.3l9.9 9.9M35.8 35.8l9.9 9.9M45.7 18.3l-9.9 9.9M28.2 35.8l-9.9 9.9" />
      </g>
    </svg>
  )
}
