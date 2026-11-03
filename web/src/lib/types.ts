export type Gender = 'Male' | 'Female' | 'Other'
export type UserRole = 'participant' | 'admin' | 'super_admin'
export type Occupation = 'Farmer' | 'Fisherman' | 'Both' | 'Allied'

export const GENDERS: Gender[] = ['Male', 'Female', 'Other']
export const OCCUPATIONS: Occupation[] = ['Farmer', 'Fisherman', 'Both', 'Allied']

export type Profile = {
  login_id: string
  full_name: string
  mobile: string
  email: string | null
  gender: string | null
  date_of_birth: string | null
  occupation: string | null
  address_line: string | null
  locality: string | null
  district: string | null
  state: string | null
  pincode: string | null
  status: string
  role: UserRole
}

export type RegistrationCreate = {
  full_name: string
  gender?: Gender | null
  date_of_birth?: string | null
  occupation?: Occupation | null
  mobile: string
  email?: string | null
  address_line?: string | null
  locality?: string | null
  district?: string | null
  state?: string | null
  pincode?: string | null
  pin: string
  verification_token: string
}

export type PhoneVerificationCreated = {
  id: string
  expires_in_seconds: number
}

export type PhoneVerificationConfirmed = {
  verification_token: string
  expires_in_seconds: number
}

export type ProfileUpdate = {
  full_name?: string | null
  email?: string | null
  occupation?: Occupation | null
  address_line?: string | null
  locality?: string | null
  district?: string | null
  state?: string | null
  pincode?: string | null
}

export type SessionResponse = {
  access_token: string
  token_type: string
  expires_at: string
  login_id: string
  full_name: string
  mobile: string
  email: string | null
  gender: string | null
  date_of_birth: string | null
  occupation: string | null
  address_line: string | null
  locality: string | null
  state: string | null
  district: string | null
  pincode: string | null
  status: string
  role: UserRole
}

export type AdminUserSummary = {
  id: string
  login_id: string
  full_name: string
  mobile: string
  district: string | null
  state: string | null
  role: UserRole
  status: string
}

export type AdminUserList = {
  items: AdminUserSummary[]
  total: number
  limit: number
  offset: number
}

export type AdminUserDetail = Profile & {
  id: string
  created_at: string
}

export type Place = {
  id: string
  name: string
  code: string
  active: boolean
  created_at: string
}

export type PlaceList = {
  items: Place[]
}

export type AttendanceRecord = {
  id: string
  place_id: string
  place_name: string
  checked_in_at: string
  attendance_date: string
  source: 'qr' | 'code'
  already_recorded: boolean
  login_id: string
  full_name: string
  mobile: string
  email: string | null
  gender: string | null
  occupation: string | null
  address_line: string | null
  locality: string | null
  district: string | null
  state: string | null
  pincode: string | null
}

export type AdminAttendanceList = {
  items: AttendanceRecord[]
  total: number
  limit: number
  offset: number
}

export type AttendanceList = {
  items: AttendanceRecord[]
  total: number
}
