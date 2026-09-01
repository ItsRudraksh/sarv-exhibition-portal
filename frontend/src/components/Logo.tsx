import sarvLogo from '../assets/sarv-bio-labs-logo.png'

export interface LogoProps {
  readonly variant?: 'header' | 'hero'
}

export function Logo({ variant = 'header' }: LogoProps) {
  return (
    <img
      src={sarvLogo}
      alt="Sarv Biolabs"
      className={`logo logo--${variant}`}
      width={variant === 'hero' ? 200 : 140}
      height={variant === 'hero' ? 48 : 32}
    />
  )
}
