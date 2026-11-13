import { describe, expect, it } from 'vitest'
import { copyFor } from './copy.ts'
import {
  ageYears,
  isIndianMobile,
  isValidIsoDate,
  joinDob,
  sectionChecks,
  validateSection,
  type RegistrationForm,
} from './registration.ts'

const copy = copyFor('en')

function iso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function yearsAgo(years: number, dayOffset = 0): string {
  const today = new Date()
  return iso(new Date(today.getFullYear() - years, today.getMonth(), today.getDate() + dayOffset))
}

function form(overrides: Partial<RegistrationForm> = {}): RegistrationForm {
  return {
    full_name: 'Anita Desai',
    gender: 'Female',
    date_of_birth: yearsAgo(30),
    occupation: 'Farmer',
    mobile: '9876543210',
    email: '',
    district: 'Nashik',
    state: 'Maharashtra',
    pincode: '422001',
    pin: '2468',
    confirmPin: '2468',
    ...overrides,
  }
}

describe('registration rules', () => {
  it('counts age from the birthday, including the 18th birthday', () => {
    expect(ageYears('not-a-date')).toBe(-1)
    expect(ageYears(yearsAgo(18))).toBe(18)
    expect(ageYears(yearsAgo(18, 1))).toBe(17)
    expect(ageYears(yearsAgo(10))).toBeLessThan(18)
  })

  it('rejects impossible dates and joins a date of birth', () => {
    expect(isValidIsoDate('2024-02-31')).toBe(false)
    expect(isValidIsoDate('2024-02-29')).toBe(true)
    expect(isValidIsoDate('2023-02-29')).toBe(false)
    expect(isValidIsoDate('03-12-1985')).toBe(false)
    expect(joinDob('1985', '03', '7')).toBe('1985-03-07')
    expect(joinDob('1985', '', '07')).toBe('')
  })

  it('accepts an Indian mobile and an optional email', () => {
    expect(isIndianMobile('9876543210')).toBe(true)
    expect(isIndianMobile('5876543210')).toBe(false)
    expect(validateSection(1, form(), copy)).toBe('')
    expect(validateSection(1, form({ email: 'anita@example.com' }), copy)).toBe('')
    expect(validateSection(1, form({ mobile: '123' }), copy)).toBe(copy.mobileRequired)
    expect(validateSection(1, form({ email: 'not-an-email' }), copy)).toBe(copy.validEmail)
  })

  it('checks the PIN length and that the confirmation matches', () => {
    expect(validateSection(3, form({ pin: '123' }), copy)).toBe(copy.passwordLength)
    expect(validateSection(3, form({ confirmPin: '9999' }), copy)).toBe(copy.passwordMatch)
    expect(validateSection(3, form(), copy)).toBe('')
  })

  it('rejects an under-18 date and an impossible date of birth', () => {
    expect(validateSection(0, form({ date_of_birth: yearsAgo(10) }), copy)).toBe(copy.minAge)
    expect(validateSection(0, form({ date_of_birth: '2024-02-31' }), copy)).toBe(copy.dobRequired)
    expect(validateSection(0, form({ full_name: ' A ' }), copy)).toBe(copy.fullNameRequired)
  })

  it('marks a section complete only when every check passes', () => {
    for (const section of [0, 1, 2, 3]) {
      expect(validateSection(section, form(), copy)).toBe('')
      expect(sectionChecks(section, form()).every(Boolean)).toBe(true)
    }
    const incomplete = form({ email: 'bad', pincode: '12', confirmPin: '' })
    expect(sectionChecks(1, incomplete)).toEqual([true, false])
    expect(validateSection(1, incomplete, copy)).not.toBe('')
    expect(sectionChecks(2, incomplete).every(Boolean)).toBe(false)
    expect(sectionChecks(3, incomplete)).toEqual([true, false])
  })
})
