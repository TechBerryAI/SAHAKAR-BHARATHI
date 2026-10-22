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

export type RegistrationCreate = {
  full_name: string
  gender?: Gender | null
  date_of_birth?: string | null
  occupation?: Occupation | null
  mobile: string
  email: string
  address_line?: string | null
  locality?: string | null
  district?: string | null
  state?: string | null
  pincode?: string | null
  password: string
}

export type ProfileUpdate = {
  full_name?: string | null
  occupation?: Occupation | null
  address_line?: string | null
  locality?: string | null
  district?: string | null
  state?: string | null
  pincode?: string | null
}
