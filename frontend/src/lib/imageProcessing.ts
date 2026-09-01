import type { CardFileMeta } from '../features/inquiry/types'

/** Max longest edge for card previews — keeps mobile browsers under memory limits. */
const MAX_EDGE_PX = 1280
const JPEG_QUALITY = 0.8

export function revokeCardPreview(meta: CardFileMeta | null | undefined): void {
  if (meta?.previewUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(meta.previewUrl)
  }
}

function parsePngSize(view: DataView): { width: number; height: number } | null {
  if (view.byteLength < 24) return null
  const png =
    view.getUint32(0) === 0x89504e47 && view.getUint32(4) === 0x0d0a1a0a
  if (!png) return null
  return {
    width: view.getUint32(16),
    height: view.getUint32(20),
  }
}

function parseJpegSize(view: DataView): { width: number; height: number } | null {
  if (view.byteLength < 4 || view.getUint16(0) !== 0xffd8) return null

  let offset = 2
  while (offset + 9 < view.byteLength) {
    if (view.getUint8(offset) !== 0xff) return null
    const marker = view.getUint8(offset + 1)
    if (marker === 0xd9) return null
    if (marker === 0xd8 || marker === 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
      offset += 2
      continue
    }
    const length = view.getUint16(offset + 2)
    if (length < 2) return null
    // SOF0 / SOF1 / SOF2
    if (marker === 0xc0 || marker === 0xc1 || marker === 0xc2) {
      return {
        height: view.getUint16(offset + 5),
        width: view.getUint16(offset + 7),
      }
    }
    offset += 2 + length
  }
  return null
}

async function readEncodedSize(
  source: Blob,
): Promise<{ width: number; height: number } | null> {
  const header = await source.slice(0, 128 * 1024).arrayBuffer()
  const view = new DataView(header)
  return parseJpegSize(view) ?? parsePngSize(view)
}

function targetSize(width: number, height: number): { width: number; height: number } {
  const longest = Math.max(width, height)
  const scale = longest > MAX_EDGE_PX ? MAX_EDGE_PX / longest : 1
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  }
}

async function loadBitmap(source: Blob): Promise<ImageBitmap> {
  if (typeof createImageBitmap !== 'function') {
    throw new Error('Image processing is not supported in this browser')
  }

  const encoded = await readEncodedSize(source)
  const size = encoded
    ? targetSize(encoded.width, encoded.height)
    : { width: MAX_EDGE_PX, height: MAX_EDGE_PX }

  const options: ImageBitmapOptions = {
    resizeWidth: size.width,
    resizeHeight: size.height,
    resizeQuality: 'low',
  }

  try {
    return await createImageBitmap(source, {
      ...options,
      imageOrientation: 'from-image',
    })
  } catch {
    try {
      return await createImageBitmap(source, options)
    } catch {
      return await createImageBitmap(source)
    }
  }
}

function sourceName(source: Blob, fallback: string): string {
  if (source instanceof File && source.name) {
    return source.name.replace(/\.[^.]+$/u, '') || fallback
  }
  return fallback
}

/**
 * Downscale during decode so full-resolution phone photos never sit in memory.
 */
export async function prepareImageForPreview(
  source: Blob,
  fallbackName = 'card-photo',
): Promise<CardFileMeta> {
  const bitmap = await loadBitmap(source)

  const canvas = document.createElement('canvas')
  canvas.width = bitmap.width
  canvas.height = bitmap.height

  try {
    const ctx = canvas.getContext('2d', { alpha: false })
    if (!ctx) {
      throw new Error('Could not prepare image preview')
    }

    ctx.drawImage(bitmap, 0, 0)

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (result) => {
          if (result) resolve(result)
          else reject(new Error('Could not compress image'))
        },
        'image/jpeg',
        JPEG_QUALITY,
      )
    })

    return {
      name: `${sourceName(source, fallbackName)}.jpg`,
      size: blob.size,
      type: 'image/jpeg',
      previewUrl: URL.createObjectURL(blob),
    }
  } finally {
    bitmap.close()
    canvas.width = 0
    canvas.height = 0
  }
}

export async function captureFrameFromVideo(
  video: HTMLVideoElement,
): Promise<Blob> {
  const width = video.videoWidth
  const height = video.videoHeight
  if (!width || !height) {
    throw new Error('Camera is not ready yet')
  }

  const size = targetSize(width, height)
  const canvas = document.createElement('canvas')
  canvas.width = size.width
  canvas.height = size.height

  try {
    const ctx = canvas.getContext('2d', { alpha: false })
    if (!ctx) {
      throw new Error('Could not capture photo')
    }
    ctx.drawImage(video, 0, 0, size.width, size.height)

    return await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (result) => {
          if (result) resolve(result)
          else reject(new Error('Could not capture photo'))
        },
        'image/jpeg',
        JPEG_QUALITY,
      )
    })
  } finally {
    canvas.width = 0
    canvas.height = 0
  }
}

export function canUseLiveCamera(): boolean {
  return (
    typeof window !== 'undefined' &&
    window.isSecureContext &&
    !!navigator.mediaDevices?.getUserMedia
  )
}
