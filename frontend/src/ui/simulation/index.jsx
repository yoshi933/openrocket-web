import { useState } from 'react';

const DEFAULT_SETTINGS = {
  windSpeed: 5,
  launchAngle: 5,
  launchAltitude: 0,
  temperature: 15,
};

function SimulationSettings({ settings, onChange }) {
  return (
    <div className="sim-settings">
      <div className="pane-header">Simulation Settings</div>
      <div className="editor-fields">
        <div className="editor-field">
          <label className="editor-field__label">Wind Speed (m/s)</label>
          <input
            className="editor-field__input"
            type="number"
            value={settings.windSpeed}
            onChange={(e) => onChange({ ...settings, windSpeed: Number(e.target.value) })}
          />
        </div>
        <div className="editor-field">
          <label className="editor-field__label">Launch Angle (°)</label>
          <input
            className="editor-field__input"
            type="number"
            value={settings.launchAngle}
            onChange={(e) => onChange({ ...settings, launchAngle: Number(e.target.value) })}
          />
        </div>
        <div className="editor-field">
          <label className="editor-field__label">Launch Altitude (m)</label>
          <input
            className="editor-field__input"
            type="number"
            value={settings.launchAltitude}
            onChange={(e) => onChange({ ...settings, launchAltitude: Number(e.target.value) })}
          />
        </div>
        <div className="editor-field">
          <label className="editor-field__label">Temperature (°C)</label>
          <input
            className="editor-field__input"
            type="number"
            value={settings.temperature}
            onChange={(e) => onChange({ ...settings, temperature: Number(e.target.value) })}
          />
        </div>
      </div>
    </div>
  );
}

function SimulationRunButton({ onRun, running }) {
  return (
    <div className="sim-run">
      <button className="sim-run__button" onClick={onRun} disabled={running}>
        {running ? 'Running…' : '▶ Run Simulation'}
      </button>
    </div>
  );
}

function SimulationGraphs({ results }) {
  if (!results) {
    return (
      <div className="sim-graphs">
        <div className="pane-header">Simulation Graphs</div>
        <p className="placeholder-text">Run a simulation to see graphs.</p>
      </div>
    );
  }
  return (
    <div className="sim-graphs">
      <div className="pane-header">Simulation Graphs</div>
      <svg width="100%" height="160" viewBox="0 0 400 140" xmlns="http://www.w3.org/2000/svg">
        {/* Altitude curve */}
        <polyline
          points="0,140 30,100 70,40 120,20 200,30 300,90 380,135 400,140"
          fill="none"
          stroke="#1565c0"
          strokeWidth="2"
        />
        <text x="200" y="130" textAnchor="middle" fontSize="10" fill="#555">
          Altitude vs Time (placeholder)
        </text>
      </svg>
      <dl className="sim-results">
        <dt>Max Altitude</dt><dd>{results.maxAltitude} m</dd>
        <dt>Max Velocity</dt><dd>{results.maxVelocity} m/s</dd>
        <dt>Flight Time</dt><dd>{results.flightTime} s</dd>
      </dl>
    </div>
  );
}

export default function Simulation() {
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [running, setRunning] = useState(false);
  const [results, setResults] = useState(null);

  function handleRun() {
    setRunning(true);
    setResults(null);
    setTimeout(() => {
      setResults({ maxAltitude: 312, maxVelocity: 87, flightTime: 18 });
      setRunning(false);
    }, 1500);
  }

  return (
    <div className="simulation-view">
      <SimulationSettings settings={settings} onChange={setSettings} />
      <div className="sim-main">
        <SimulationRunButton onRun={handleRun} running={running} />
        <SimulationGraphs results={results} />
      </div>
    </div>
  );
}
