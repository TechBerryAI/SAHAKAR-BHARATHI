import { describe, expect, it } from 'vitest'
import { blankToNull, firstName, formatMobile, initials, regionLabel } from './format.ts'

describe('format helpers', () => {
  it('builds initials and falls back to SB', () => {
    expect(initials('  anita   desai  ')).toBe('AD')
    expect(initials('Ravi')).toBe('R')
    expect(initials('   ')).toBe('SB')
  })

  it('reads the first name', () => {
    expect(firstName('  Anita Desai ')).toBe('Anita')
    expect(firstName('   ')).toBe('')
  })

  it('joins district and state, skipping blanks', () => {
    expect(regionLabel('Nashik', 'Maharashtra')).toBe('Nashik, Maharashtra')
    expect(regionLabel(null, 'Maharashtra')).toBe('Maharashtra')
    expect(regionLabel('', null)).toBe('')
  })

  it('prefixes a 10-digit mobile and leaves other values alone', () => {
    expect(formatMobile('9876543210')).toBe('+91 9876543210')
    expect(formatMobile('+91 9876543210')).toBe('+91 9876543210')
  })

  it('turns blank text into null', () => {
    expect(blankToNull('  Ward 4  ')).toBe('Ward 4')
    expect(blankToNull('   ')).toBeNull()
  })
})
