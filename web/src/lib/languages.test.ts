import { describe, expect, it } from 'vitest'
import { LANGUAGE_CODES, isLangCode, languageByCode } from './languages.ts'

describe('languages', () => {
  it('accepts known codes and rejects anything else', () => {
    expect(isLangCode('hi')).toBe(true)
    expect(isLangCode('en')).toBe(true)
    expect(isLangCode('xx')).toBe(false)
    expect(isLangCode(null)).toBe(false)
    expect(isLangCode('')).toBe(false)
  })

  it('looks up a language and marks the right-to-left ones', () => {
    expect(languageByCode('hi').native).toBe('हिन्दी')
    expect(languageByCode('en').rtl).toBeUndefined()
    const rtl = LANGUAGE_CODES.filter((code) => languageByCode(code).rtl).sort()
    expect(rtl).toEqual(['ks', 'sd', 'ur'])
  })
})
