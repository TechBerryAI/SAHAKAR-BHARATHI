import QRCode from 'qrcode'

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

/** SVG placard for `code`. Does not use a canvas, so privacy browsers can still show it. */
export async function boothQrSvg(code: string): Promise<string> {
  const text = boothQrText(code)
  if (!boothCodeFromQr(text)) {
    throw new Error('QR could not be drawn.')
  }
  const svg = await QRCode.toString(text, {
    type: 'svg',
    margin: 1,
    width: 320,
    errorCorrectionLevel: 'M',
    color: { dark: '#000000', light: '#ffffff' },
  })
  const marked = svg.replace(/<svg\b/, `<svg data-booth="${text}"`)
  if (!marked.includes(`data-booth="${text}"`)) {
    throw new Error('QR could not be drawn.')
  }
  return marked.replace(/^\s*<\?xml[\s\S]*?\?>\s*/, '')
}
