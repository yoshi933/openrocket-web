import React, { useEffect, useState } from 'react';
import { loadWasm, type SimulationExports } from './wasm/loader';
import {
  defaultSimulationInput,
  simulate1D,
  type SimulationInput,
  type SimulationResult
} from './sim/sim';

function App() {
  const [wasmExports, setWasmExports] = useState<SimulationExports | null>(null);
  const [inputJson, setInputJson] = useState<string>(
    JSON.stringify(defaultSimulationInput, null, 2)
  );
  const [resultJson, setResultJson] = useState<string | null>(null);
  const [backendResult, setBackendResult] = useState<SimulationResult | null>(null);
  const [jsResult, setJsResult] = useState<SimulationResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const exports = await loadWasm();
        setWasmExports(exports);
      } catch (e) {
        console.error('WASM load failed', e);
      }
    })();
  }, []);

  const handleRunSimulation = async () => {
    if (!wasmExports) return;

    try {
      setError(null);
      const parsed = JSON.parse(inputJson) as Partial<SimulationInput>;
      const input: SimulationInput = { ...defaultSimulationInput, ...parsed };

      setJsResult(simulate1D(input));
      const raw = wasmExports.runSimulation(inputJson);
      setResultJson(raw);
      try {
        setBackendResult(JSON.parse(raw) as SimulationResult);
      } catch {
        setBackendResult(null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const altitudeSeries = jsResult?.timeSeries
    ? jsResult.timeSeries.time.map((t, i) => ({
        x: t,
        y: jsResult.timeSeries.altitude[i] ?? 0
      }))
    : null;

  return (
    <div style={{ padding: 20 }}>
      <h1>OpenRocket Web Edition (UI)</h1>
      <p>Backend: {wasmExports ? wasmExports.__kind : 'loading'}</p>
      <button onClick={handleRunSimulation} disabled={!wasmExports}>
        Run Simulation
      </button>
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
      <div style={{ display: 'grid', gap: 12, marginTop: 12, maxWidth: 900 }}>
        <label style={{ display: 'grid', gap: 6 }}>
          <span>Simulation input (JSON)</span>
          <textarea
            value={inputJson}
            onChange={(e) => setInputJson(e.target.value)}
            rows={12}
            style={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}
          />
        </label>

        {jsResult && (
          <div style={{ display: 'grid', gap: 8 }}>
            <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
              <div>JS Max Altitude: {jsResult.summary.maxAltitude.toFixed(2)} m</div>
              <div>JS Max Velocity: {jsResult.summary.maxVelocity.toFixed(2)} m/s</div>
              <div>JS Flight Time: {jsResult.summary.flightTime.toFixed(2)} s</div>
              <div>JS Apogee Time: {jsResult.summary.apogeeTime.toFixed(2)} s</div>
            </div>
            {backendResult && (
              <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', opacity: 0.9 }}>
                <div>Backend Max Altitude: {backendResult.summary.maxAltitude.toFixed(2)} m</div>
                <div>Backend Max Velocity: {backendResult.summary.maxVelocity.toFixed(2)} m/s</div>
                <div>Backend Flight Time: {backendResult.summary.flightTime.toFixed(2)} s</div>
                <div>Backend Apogee Time: {backendResult.summary.apogeeTime.toFixed(2)} s</div>
              </div>
            )}
            {altitudeSeries && (
              <AltitudePlot
                points={altitudeSeries}
                title="Altitude vs Time (m)"
              />
            )}
          </div>
        )}

        {resultJson && (
          <details>
            <summary>Raw result JSON</summary>
            <pre style={{ whiteSpace: 'pre-wrap' }}>{resultJson}</pre>
          </details>
        )}
      </div>
      {!wasmExports && <p>WASM モジュールをロード中、もしくは未設定です。</p>}
    </div>
  );
}

function AltitudePlot(props: { points: { x: number; y: number }[]; title: string }) {
  const { points, title } = props;
  if (points.length < 2) return null;

  const width = 700;
  const height = 220;
  const padding = 30;

  const xs = points.map((p) => p.x);
  const ys = points.map((p) => p.y);
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);

  const rangeX = maxX - minX || 1;
  const rangeY = maxY - minY || 1;

  const toSvgX = (x: number) => padding + ((x - minX) / rangeX) * (width - padding * 2);
  const toSvgY = (y: number) =>
    height - padding - ((y - minY) / rangeY) * (height - padding * 2);

  const polylinePoints = points
    .map((p) => `${toSvgX(p.x).toFixed(2)},${toSvgY(p.y).toFixed(2)}`)
    .join(' ');

  return (
    <svg
      width="100%"
      viewBox={`0 0 ${width} ${height}`}
      style={{ border: '1px solid #ddd', borderRadius: 6, background: '#fff' }}
      role="img"
      aria-label={title}
    >
      <text x={padding} y={18} fontSize="12" fill="#333">
        {title}
      </text>
      <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="#aaa" />
      <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="#aaa" />
      <polyline fill="none" stroke="#2563eb" strokeWidth="2" points={polylinePoints} />
    </svg>
  );
}

export default App;
