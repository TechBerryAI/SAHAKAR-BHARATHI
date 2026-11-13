import jsQR from 'jsqr'
import { boothCodeFromQr } from './boothCode.ts'

export const SCAN_NOTE_WAIT = 'Point the camera at the booth QR, then tap Scan QR.'
export const SCAN_NOTE_EMPTY = 'No booth QR in the frame. Move closer and tap Scan QR.'
export const SCAN_NOTE_OTHER = 'This QR has no booth code. The 6-digit code is printed under the booth QR.'
export const SCAN_NOTE_FAIL = 'The QR could not be read. Move closer and tap Scan QR.'

const MAX_EDGE = 480

/** Reads one video frame. Never throws: an empty, foreign, or unreadable frame returns a note. */
export function readBoothQr(
  video: HTMLVideoElement,
  canvas: HTMLCanvasElement,
): { raw: string | null; note: string } {
  try {
    if (video.readyState < 2 || video.videoWidth < 2 || video.videoHeight < 2) {
      return { raw: null, note: SCAN_NOTE_WAIT }
    }
    const scale = Math.min(1, MAX_EDGE / Math.max(video.videoWidth, video.videoHeight))
    const width = Math.max(1, Math.round(video.videoWidth * scale))
    const height = Math.max(1, Math.round(video.videoHeight * scale))
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) return { raw: null, note: SCAN_NOTE_FAIL }
    canvas.width = width
    canvas.height = height
    context.drawImage(video, 0, 0, width, height)
    const image = context.getImageData(0, 0, width, height)
    const found = jsQR(image.data, image.width, image.height, { inversionAttempts: 'attemptBoth' })
    const raw = found?.data ?? ''
    if (!raw) return { raw: null, note: SCAN_NOTE_EMPTY }
    if (!boothCodeFromQr(raw)) return { raw: null, note: SCAN_NOTE_OTHER }
    return { raw, note: '' }
  } catch {
    return { raw: null, note: SCAN_NOTE_FAIL }
  }
}
