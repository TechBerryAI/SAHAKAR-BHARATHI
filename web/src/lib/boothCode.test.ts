import { describe, expect, it } from 'vitest'
import { boothCodeFromManual, boothCodeFromQr, boothQrText } from './boothCode.ts'

describe('booth codes', () => {
  it('accepts only the printed QR payload', () => {
    expect(boothCodeFromQr('SB-PLACE:004218')).toBe('004218')
    expect(boothCodeFromQr('  SB-PLACE:004218\n')).toBe('004218')
    expect(boothCodeFromQr('https://example.com/004218')).toBeNull()
    expect(boothCodeFromQr('004218')).toBeNull()
  })

  it('accepts a typed 6-digit code', () => {
    expect(boothCodeFromManual('004218')).toBe('004218')
    expect(boothCodeFromManual('SB-PLACE:004218')).toBeNull()
    expect(boothCodeFromManual('4218')).toBeNull()
  })

  it('prints the same text the phone scanner accepts', () => {
    expect(boothQrText('004218')).toBe('SB-PLACE:004218')
    expect(boothCodeFromQr(boothQrText('004218'))).toBe('004218')
  })
})
