/**
 * Decorative Himalayan + molecule artwork in Sarv navy/cyan.
 * No people, products, or invented claims — brand atmosphere only.
 */
export function BrandArt() {
  return (
    <svg
      className="brand-art"
      viewBox="0 0 720 280"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id="brand-sky" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#022d59" />
          <stop offset="55%" stopColor="#01487a" />
          <stop offset="100%" stopColor="#01afef" />
        </linearGradient>
        <linearGradient id="brand-ridge" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#4aa485" stopOpacity="0.45" />
          <stop offset="100%" stopColor="#022d59" stopOpacity="0.15" />
        </linearGradient>
      </defs>
      <rect width="720" height="280" fill="url(#brand-sky)" />
      <g fill="none" stroke="#ffffff" strokeOpacity="0.18" strokeWidth="1.4">
        <polygon points="86,54 116,71 116,105 86,122 56,105 56,71" />
        <polygon points="188,36 214,51 214,81 188,96 162,81 162,51" />
        <polygon points="612,48 644,66 644,102 612,120 580,102 580,66" />
        <circle cx="140" cy="88" r="4" fill="#01afef" stroke="none" />
        <circle cx="86" cy="54" r="3.5" fill="#ffffff" stroke="none" />
        <circle cx="612" cy="48" r="3.5" fill="#ffffff" stroke="none" />
        <line x1="116" y1="71" x2="162" y2="51" />
        <line x1="188" y1="96" x2="140" y2="88" />
      </g>
      <path
        d="M0 210 C80 168 140 186 210 150 C280 114 330 168 410 128 C490 88 540 150 620 118 C680 96 710 130 720 122 L720 280 L0 280 Z"
        fill="url(#brand-ridge)"
      />
      <path
        d="M0 232 C90 204 170 244 260 214 C350 184 420 236 510 208 C600 180 660 220 720 204 L720 280 L0 280 Z"
        fill="#011c38"
        fillOpacity="0.45"
      />
    </svg>
  )
}
