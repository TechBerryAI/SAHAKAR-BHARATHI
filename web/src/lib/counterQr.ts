import QRCode from 'qrcode'

const payload = /^SB-COUNTER:([A-Za-z0-9_-]{32,64})$/

/** Counter placards encode `SB-COUNTER:` plus an opaque token. Booth placards are ignored. */
export function counterQrFromPayload(raw: string): string | null {
  return payload.exec(raw.trim())?.[1] ?? null
}

export function counterQrText(token: string): string {
  return `SB-COUNTER:${token}`
}

export async function counterQrSvg(qrText: string): Promise<string> {
  if (!counterQrFromPayload(qrText)) {
    throw new Error('QR could not be drawn.')
  }
  const svg = await QRCode.toString(qrText, {
    type: 'svg',
    margin: 1,
    width: 320,
    errorCorrectionLevel: 'M',
    color: { dark: '#000000', light: '#ffffff' },
  })
  const marked = svg.replace(/<svg\b/, `<svg data-counter="${qrText}"`)
  if (!marked.includes(`data-counter="${qrText}"`)) {
    throw new Error('QR could not be drawn.')
  }
  return marked.replace(/^\s*<\?xml[\s\S]*?\?>\s*/, '')
}
