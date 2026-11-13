import type { AppCopy } from './copy.ts'
import type { Gender, Occupation } from './types.ts'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PIN_PATTERN = /^\d{4,6}$/
const PINCODE_PATTERN = /^\d{6}$/
const INDIAN_MOBILE = /^[6-9]\d{9}$/

export type RegistrationForm = {
  full_name: string
  gender: Gender | ''
  date_of_birth: string
  occupation: Occupation | ''
  mobile: string
  email: string
  district: string
  state: string
  pincode: string
  pin: string
  confirmPin: string
}

export function isIndianMobile(mobile: string): boolean {
  return INDIAN_MOBILE.test(mobile)
}

export function ageYears(iso: string): number {
  const dob = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(dob.getTime())) return -1
  const today = new Date()
  let years = today.getFullYear() - dob.getFullYear()
  const beforeBirthday =
    today.getMonth() < dob.getMonth() ||
    (today.getMonth() === dob.getMonth() && today.getDate() < dob.getDate())
  if (beforeBirthday) years -= 1
  return years
}

export function isValidIsoDate(iso: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  if (!match) return false
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(year, month - 1, day)
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
}

export function joinDob(year: string, month: string, day: string) {
  if (!year || !month || !day) return ''
  return `${year}-${month}-${day.padStart(2, '0')}`
}

export function validateSection(section: number, form: RegistrationForm, copy: AppCopy): string {
  if (section === 0) {
    if (form.full_name.trim().length < 2) return copy.fullNameRequired
    if (!form.gender) return copy.genderRequired
    if (!form.date_of_birth || !isValidIsoDate(form.date_of_birth)) return copy.dobRequired
    if (ageYears(form.date_of_birth) < 18) return copy.minAge
    if (!form.occupation) return copy.occupationRequired
    return ''
  }
  if (section === 1) {
    if (!isIndianMobile(form.mobile)) return copy.mobileRequired
    if (form.email.trim() && !EMAIL_PATTERN.test(form.email.trim())) return copy.validEmail
    return ''
  }
  if (section === 2) {
    if (!form.district.trim()) return copy.districtRequired
    if (!form.state.trim()) return copy.stateRequired
    if (!PINCODE_PATTERN.test(form.pincode)) return copy.pinDigits
    return ''
  }
  if (!PIN_PATTERN.test(form.pin)) return copy.passwordLength
  if (form.pin !== form.confirmPin) return copy.passwordMatch
  return ''
}

// Required items per section, mirroring validateSection: a section is complete
// exactly when all of its items pass. Optional fields only count when they are invalid.
export function sectionChecks(section: number, form: RegistrationForm): boolean[] {
  if (section === 0) {
    const dobOk = isValidIsoDate(form.date_of_birth) && ageYears(form.date_of_birth) >= 18
    return [form.full_name.trim().length >= 2, Boolean(form.gender), dobOk, Boolean(form.occupation)]
  }
  if (section === 1) {
    const emailOk = !form.email.trim() || EMAIL_PATTERN.test(form.email.trim())
    return [isIndianMobile(form.mobile), emailOk]
  }
  if (section === 2) {
    return [Boolean(form.district.trim()), Boolean(form.state.trim()), PINCODE_PATTERN.test(form.pincode)]
  }
  return [PIN_PATTERN.test(form.pin), form.confirmPin !== '' && form.confirmPin === form.pin]
}
