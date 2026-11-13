import { describe, expect, it } from 'vitest'
import { copyFor, occupationLabel, statusLabel } from './copy.ts'
import { LANGUAGE_CODES } from './languages.ts'

describe('copy', () => {
  it('labels occupations and account status, and keeps unknown values', () => {
    const copy = copyFor('en')
    expect(occupationLabel(copy, 'Farmer')).toBe(copy.farmer)
    expect(occupationLabel(copy, 'Fisherman')).toBe(copy.fisherman)
    expect(occupationLabel(copy, 'Both')).toBe(copy.both)
    expect(occupationLabel(copy, 'Allied')).toBe(copy.allied)
    expect(occupationLabel(copy, 'Teacher')).toBe('Teacher')
    expect(statusLabel(copy, 'active')).toBe(copy.statusActive)
    expect(statusLabel(copy, 'disabled')).toBe(copy.statusDisabled)
    expect(statusLabel(copy, 'pending')).toBe('pending')
  })

  it('gives every language the same keys as English', () => {
    const english = Object.keys(copyFor('en')).sort()
    for (const code of LANGUAGE_CODES) {
      expect(Object.keys(copyFor(code)).sort(), code).toEqual(english)
    }
  })
})
