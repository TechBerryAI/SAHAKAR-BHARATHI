export type Gender = 'Male' | 'Female' | 'Other'
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
}

export type SessionResponse = {
  access_token: string
  token_type: string
  expires_at: string
  login_id: string
  full_name: string
  mobile: string
  occupation: string | null
  state: string | null
  district: string | null
}

export type RegistrationResponse = {
  registration_id: string
  login_id: string
  full_name: string
  mobile: string
  occupation: string | null
  state: string | null
  district: string | null
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

export type SessionCreate = {
  identifier: string
  pin: string
}

export function profileFromSession(session: SessionResponse): Profile {
  return {
    login_id: session.login_id,
    full_name: session.full_name,
    mobile: session.mobile,
    email: null,
    gender: null,
    date_of_birth: null,
    occupation: session.occupation,
    address_line: null,
    locality: null,
    district: session.district,
    state: session.state,
    pincode: null,
    status: 'active',
  }
}
