import { beforeEach, describe, expect, it, vi } from 'vitest'

const jsQR = vi.hoisted(() => vi.fn())
vi.mock('jsqr', () => ({ default: jsQR }))

import {
  SCAN_NOTE_EMPTY,
  SCAN_NOTE_FAIL,
  SCAN_NOTE_OTHER,
  SCAN_NOTE_WAIT,
  readBoothQr,
} from './scanFrame.ts'

function frame(ready = true) {
  const image = { data: new Uint8ClampedArray(16), width: 2, height: 2 }
  const context = {
    drawImage: vi.fn(),
    getImageData: vi.fn(() => image),
  }
  const canvas = {
    width: 0,
    height: 0,
    getContext: vi.fn(() => context),
  }
  const video = {
    readyState: ready ? 2 : 1,
    videoWidth: ready ? 960 : 0,
    videoHeight: ready ? 480 : 0,
  }
  return {
    video: video as unknown as HTMLVideoElement,
    canvas: canvas as unknown as HTMLCanvasElement,
    context,
    image,
  }
}

beforeEach(() => {
  jsQR.mockReset()
})

describe('readBoothQr', () => {
  it('waits until the camera has a frame', () => {
    const { video, canvas } = frame(false)
    expect(readBoothQr(video, canvas)).toEqual({ raw: null, note: SCAN_NOTE_WAIT })
    expect(jsQR).not.toHaveBeenCalled()
  })

  it('returns a booth payload, a foreign code, or an empty frame', () => {
    const found = frame()
    jsQR.mockReturnValueOnce({ data: 'SB-PLACE:004218' })
    expect(readBoothQr(found.video, found.canvas)).toEqual({ raw: 'SB-PLACE:004218', note: '' })
    expect(jsQR).toHaveBeenCalledWith(found.image.data, found.image.width, found.image.height, {
      inversionAttempts: 'attemptBoth',
    })
    expect(found.context.drawImage).toHaveBeenCalledWith(found.video, 0, 0, 480, 240)

    jsQR.mockReturnValueOnce({ data: 'https://example.com' })
    expect(readBoothQr(found.video, found.canvas).note).toBe(SCAN_NOTE_OTHER)

    jsQR.mockReturnValueOnce(null)
    expect(readBoothQr(found.video, found.canvas)).toEqual({ raw: null, note: SCAN_NOTE_EMPTY })
  })

  it('returns a failure note when the canvas or decoder throws', () => {
    const missing = frame()
    missing.canvas.getContext = () => null
    expect(readBoothQr(missing.video, missing.canvas)).toEqual({ raw: null, note: SCAN_NOTE_FAIL })

    const broken = frame()
    jsQR.mockImplementationOnce(() => {
      throw new Error('decoder')
    })
    expect(readBoothQr(broken.video, broken.canvas)).toEqual({ raw: null, note: SCAN_NOTE_FAIL })
  })
})
