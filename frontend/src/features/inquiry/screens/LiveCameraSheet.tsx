import { useEffect, useRef, useState } from 'react'
import { copy } from '../copy'
import { captureFrameFromVideo } from '../../../lib/imageProcessing'

export interface LiveCameraSheetProps {
  readonly onCapture: (blob: Blob) => void
  readonly onCancel: () => void
  readonly onUnavailable: () => void
}

export function LiveCameraSheet({
  onCapture,
  onCancel,
  onUnavailable,
}: LiveCameraSheetProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const onUnavailableRef = useRef(onUnavailable)
  onUnavailableRef.current = onUnavailable

  useEffect(() => {
    let cancelled = false

    const start = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1280 },
            height: { ideal: 720 },
          },
        })
        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop())
          return
        }
        streamRef.current = stream
        const video = videoRef.current
        if (video) {
          video.srcObject = stream
          await video.play()
        }
      } catch {
        if (!cancelled) onUnavailableRef.current()
      }
    }

    void start()

    return () => {
      cancelled = true
      streamRef.current?.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }
  }, [])

  const stopCamera = () => {
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
  }

  const handleCapture = async () => {
    const video = videoRef.current
    if (!video || busy) return
    setBusy(true)
    setError(null)
    try {
      const blob = await captureFrameFromVideo(video)
      stopCamera()
      onCapture(blob)
    } catch {
      setError(copy.cardCapture.processingFailed)
      setBusy(false)
    }
  }

  const handleCancel = () => {
    stopCamera()
    onCancel()
  }

  return (
    <div className="camera-sheet" role="dialog" aria-modal="true" aria-label={copy.cardCapture.useCamera}>
      <video
        ref={videoRef}
        className="camera-sheet__video"
        playsInline
        muted
        autoPlay
      />
      {error ? (
        <p className="camera-sheet__error" role="alert">
          {error}
        </p>
      ) : null}
      <div className="camera-sheet__bar">
        <button type="button" className="btn btn-secondary" onClick={handleCancel}>
          {copy.common.back}
        </button>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => void handleCapture()}
          disabled={busy}
        >
          {busy ? copy.cardCapture.processing : copy.cardCapture.takePhoto}
        </button>
      </div>
    </div>
  )
}
