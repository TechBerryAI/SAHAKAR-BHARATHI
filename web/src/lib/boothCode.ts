const payload = /^SB-PLACE:(\d{6})$/

/** Booth placards encode `SB-PLACE:` plus the 6-digit code. Anything else is ignored. */
export function boothCodeFromQr(raw: string): string | null {
  return payload.exec(raw.trim())?.[1] ?? null
}

export function boothCodeFromManual(raw: string): string | null {
  const digits = raw.trim()
  return /^\d{6}$/.test(digits) ? digits : null
}

export function boothQrText(code: string): string {
  return `SB-PLACE:${code}`
}
