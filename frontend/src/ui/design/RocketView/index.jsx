export default function RocketView() {
  return (
    <div className="rocket-view">
      <div className="pane-header">2D Side View</div>
      <div className="rocket-view__canvas">
        <svg
          width="100%"
          height="100%"
          viewBox="0 0 600 200"
          xmlns="http://www.w3.org/2000/svg"
          aria-label="2D rocket side view"
        >
          {/* Nose Cone */}
          <polygon points="100,100 200,60 200,140" fill="#90caf9" stroke="#1565c0" strokeWidth="1.5" />
          {/* Body Tube */}
          <rect x="200" y="60" width="250" height="80" fill="#bbdefb" stroke="#1565c0" strokeWidth="1.5" />
          {/* Fins */}
          <polygon points="420,140 450,180 480,140" fill="#64b5f6" stroke="#1565c0" strokeWidth="1.5" />
          <polygon points="420,60 450,20 480,60" fill="#64b5f6" stroke="#1565c0" strokeWidth="1.5" />
          {/* Motor Mount */}
          <rect x="450" y="75" width="30" height="50" fill="#e3f2fd" stroke="#1565c0" strokeWidth="1.5" />
          {/* Placeholder label */}
          <text x="300" y="115" textAnchor="middle" fontSize="12" fill="#555">
            Rocket Geometry (placeholder)
          </text>
        </svg>
      </div>
    </div>
  );
}
