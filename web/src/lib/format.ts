export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean).slice(0, 2)
  const letters = parts.map((part) => part[0]?.toUpperCase() ?? '').join('')
  return letters || 'SB'
}

export function firstName(name: string): string {
  return name.trim().split(/\s+/).filter(Boolean)[0] ?? ''
}

export function regionLabel(district: string | null, state: string | null): string {
  return [district, state].filter(Boolean).join(', ')
}

export function formatMobile(mobile: string): string {
  return mobile.length === 10 ? `+91 ${mobile}` : mobile
}

export function blankToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}
