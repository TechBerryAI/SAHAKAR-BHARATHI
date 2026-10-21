import { FirebaseError } from 'firebase/app'
import {
  createUserWithEmailAndPassword,
  deleteUser,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth'
import { doc, getDoc, serverTimestamp, updateDoc, writeBatch } from 'firebase/firestore'
import { auth, db } from './firebase.ts'
import { clearSession, getSnapshot, setProfile, setUser } from './session.ts'
import type { Profile, ProfileUpdate, RegistrationCreate } from './types.ts'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

function generateLoginId(): string {
  const bytes = new Uint8Array(4)
  crypto.getRandomValues(bytes)
  return `SB-${Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('').toUpperCase()}`
}

function asString(value: unknown): string | null {
  return typeof value === 'string' && value.length ? value : null
}

export function profileFromDoc(data: Record<string, unknown>): Profile {
  return {
    login_id: String(data.login_id ?? ''),
    full_name: String(data.full_name ?? ''),
    mobile: String(data.mobile ?? ''),
    email: asString(data.email),
    gender: asString(data.gender),
    date_of_birth: asString(data.date_of_birth),
    occupation: asString(data.occupation),
    address_line: asString(data.address_line),
    locality: asString(data.locality),
    district: asString(data.district),
    state: asString(data.state),
    pincode: asString(data.pincode),
    status: String(data.status ?? 'active'),
  }
}

function mapFirebaseError(err: unknown, context: 'auth' | 'register' | 'profile' = 'auth'): ApiError {
  if (err instanceof ApiError) return err
  if (err instanceof FirebaseError) {
    switch (err.code) {
      case 'auth/email-already-in-use':
      case 'already-exists':
        return new ApiError(409, 'An account with these details already exists.')
      case 'permission-denied':
        return context === 'register'
          ? new ApiError(409, 'An account with these details already exists.')
          : new ApiError(403, 'You do not have access to this profile.')
      case 'auth/invalid-credential':
      case 'auth/invalid-email':
      case 'auth/user-not-found':
      case 'auth/wrong-password':
      case 'auth/user-disabled':
        return new ApiError(401, 'Email or password is incorrect.')
      case 'auth/weak-password':
        return new ApiError(422, 'Password must be at least 6 characters.')
      case 'auth/too-many-requests':
        return new ApiError(429, 'Too many attempts. Try again later.')
      case 'auth/network-request-failed':
      case 'unavailable':
        return new ApiError(0, 'Cannot reach Firebase. Check your connection.')
      case 'auth/operation-not-allowed':
        return new ApiError(500, 'Email/password sign-in is not enabled.')
      default:
        return new ApiError(400, 'Something went wrong. Try again.')
    }
  }
  return new ApiError(400, 'Something went wrong. Try again.')
}

function requireUid(): string {
  const uid = getSnapshot().uid ?? auth.currentUser?.uid ?? null
  if (!uid) throw new ApiError(401, 'Authentication required.')
  return uid
}

export async function getMe(): Promise<Profile> {
  try {
    const uid = requireUid()
    const snap = await getDoc(doc(db, 'users', uid))
    if (!snap.exists()) {
      throw new ApiError(404, 'Could not load profile.')
    }
    const profile = profileFromDoc(snap.data())
    setProfile(profile)
    return profile
  } catch (err) {
    throw mapFirebaseError(err, 'profile')
  }
}

export async function updateMe(body: ProfileUpdate): Promise<Profile> {
  try {
    const uid = requireUid()
    await updateDoc(doc(db, 'users', uid), {
      full_name: body.full_name ?? null,
      occupation: body.occupation ?? null,
      address_line: body.address_line ?? null,
      locality: body.locality ?? null,
      district: body.district ?? null,
      state: body.state ?? null,
      pincode: body.pincode ?? null,
      updated_at: serverTimestamp(),
    })
    return await getMe()
  } catch (err) {
    throw mapFirebaseError(err, 'profile')
  }
}

export async function signIn(email: string, password: string): Promise<Profile> {
  try {
    const credential = await signInWithEmailAndPassword(auth, email.trim().toLowerCase(), password)
    setUser(credential.user)
    return await getMe()
  } catch (err) {
    throw mapFirebaseError(err)
  }
}

async function writeRegistration(uid: string, email: string, body: RegistrationCreate): Promise<Profile> {
  let lastError: unknown
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const loginId = generateLoginId()
    const batch = writeBatch(db)
    const profile = {
      login_id: loginId,
      full_name: body.full_name,
      mobile: body.mobile,
      email,
      gender: body.gender ?? null,
      date_of_birth: body.date_of_birth ?? null,
      occupation: body.occupation ?? null,
      address_line: body.address_line ?? null,
      locality: body.locality ?? null,
      district: body.district ?? null,
      state: body.state ?? null,
      pincode: body.pincode ?? null,
      status: 'active',
    }
    batch.set(doc(db, 'users', uid), {
      ...profile,
      created_at: serverTimestamp(),
      updated_at: serverTimestamp(),
    })
    batch.set(doc(db, 'loginIds', loginId), { uid })
    batch.set(doc(db, 'mobiles', body.mobile), { uid })
    try {
      await batch.commit()
      setProfile(profile)
      return profile
    } catch (err) {
      lastError = err
    }
  }
  throw lastError
}

export async function registerAndSignIn(body: RegistrationCreate): Promise<Profile> {
  const email = body.email.trim().toLowerCase()
  let createdUid: string | null = null
  try {
    const credential = await createUserWithEmailAndPassword(auth, email, body.password)
    createdUid = credential.user.uid
    setUser(credential.user)
    return await writeRegistration(createdUid, email, body)
  } catch (err) {
    if (createdUid && auth.currentUser) {
      try {
        await deleteUser(auth.currentUser)
      } catch {
        await signOut(auth)
      }
    }
    throw mapFirebaseError(err, 'register')
  }
}

export async function logout(): Promise<void> {
  try {
    await signOut(auth)
  } finally {
    clearSession()
  }
}
