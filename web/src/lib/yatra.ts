export type YatraStatus =
  | 'draft'
  | 'planned'
  | 'approval_pending'
  | 'approved'
  | 'upcoming'
  | 'in_progress'
  | 'completed'
  | 'archived'

export type ProtocolCategory = 'pre' | 'during' | 'post'

export type ProtocolStatus =
  | 'pending'
  | 'submitted'
  | 'under_review'
  | 'approved'
  | 'rejected'
  | 'resubmitted'

export type DistrictBrief = {
  id: string
  name: string
  code: string
}

export type AdministratorBrief = {
  user_id: string
  login_id: string
  full_name: string
}

export type YatraCapabilities = {
  edit: boolean
  transition: boolean
  publish: boolean
  assign_admin: boolean
  manage_stops: boolean
  submit_protocol: boolean
  review_protocol: boolean
  manage_items: boolean
}

export type YatraStop = {
  id: string
  sequence: number
  name: string
  address: string | null
  latitude: number | null
  longitude: number | null
  planned_arrival: string | null
  planned_departure: string | null
  active: boolean
}

export type YatraSummary = {
  id: string
  public_code: string
  name: string
  district: DistrictBrief
  starts_at: string
  ends_at: string
  status: YatraStatus
  published: boolean
}

export type YatraDetail = YatraSummary & {
  description: string | null
  next_status: YatraStatus | null
  capabilities: YatraCapabilities
  administrators: AdministratorBrief[]
  stops: YatraStop[]
  created_at: string
  updated_at: string
}

export type YatraPage = {
  items: YatraSummary[]
  total: number
  limit: number
  offset: number
}

export type District = {
  id: string
  name: string
  code: string
  state_name: string | null
  active: boolean
  administrators: AdministratorBrief[]
  created_at: string
  updated_at: string
}

export type DistrictPage = {
  items: District[]
  total: number
  limit: number
  offset: number
}

export type ProtocolDocument = {
  id: string
  original_filename: string
  content_type: string
  size_bytes: number
  created_at: string
}

export type ProtocolItem = {
  id: string
  category: ProtocolCategory
  title: string
  description: string | null
  required: boolean
  status: ProtocolStatus
  due_at: string | null
  overdue: boolean
  responsible_label: string | null
  submitted_at: string | null
  reviewed_at: string | null
  rejection_reason: string | null
  documents: ProtocolDocument[]
  created_at: string
  updated_at: string
}

export type YatraRoute = {
  label: string | null
  geometry: string | null
  updated_at: string
}

const STATUS_LABELS: Record<YatraStatus, string> = {
  draft: 'Draft',
  planned: 'Planned',
  approval_pending: 'Approval pending',
  approved: 'Approved',
  upcoming: 'Upcoming',
  in_progress: 'In progress',
  completed: 'Completed',
  archived: 'Archived',
}

const PROTOCOL_LABELS: Record<ProtocolStatus, string> = {
  pending: 'Pending',
  submitted: 'Submitted',
  under_review: 'Under review',
  approved: 'Approved',
  rejected: 'Rejected',
  resubmitted: 'Resubmitted',
}

const CATEGORY_LABELS: Record<ProtocolCategory, string> = {
  pre: 'Pre-Yatra',
  during: 'During Yatra',
  post: 'Post-Yatra',
}

export function yatraStatusLabel(status: string): string {
  return STATUS_LABELS[status as YatraStatus] ?? status
}

export function protocolStatusLabel(status: string): string {
  return PROTOCOL_LABELS[status as ProtocolStatus] ?? status
}

export function protocolCategoryLabel(category: string): string {
  return CATEGORY_LABELS[category as ProtocolCategory] ?? category
}

export function formatWhen(value: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export function toApiInstant(localValue: string): string {
  return new Date(localValue).toISOString()
}

export function documentOpen(status: ProtocolStatus): boolean {
  return status === 'pending' || status === 'rejected' || status === 'submitted' || status === 'resubmitted'
}
