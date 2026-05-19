import React, { useEffect, useState } from 'react';
import { loadWasm, type SimulationExports } from './wasm/loader';
import {
  defaultSimulationInput,
  simulate1D,
  type SimulationInput,
  type SimulationResult
} from './sim/sim';

const LOCAL_STORAGE_KEY = 'openrocket-web:simulationInput:v1';

function tryLoadSavedInput(): SimulationInput | null {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<SimulationInput>;
    return { ...defaultSimulationInput, ...parsed };
  } catch {
    return null;
  }
}

function saveInput(input: SimulationInput) {
  try {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(input));
  } catch {
    // ignore
  }
}

function App() {
  const [wasmExports, setWasmExports] = useState<SimulationExports | null>(null);
  const [input, setInput] = useState<SimulationInput>(() => tryLoadSavedInput() ?? defaultSimulationInput);
  const [inputMode, setInputMode] = useState<'form' | 'json'>('form');
  const [activePane, setActivePane] = useState<'inputs' | 'results'>('inputs');
  const [inputJson, setInputJson] = useState<string>(() => JSON.stringify(input, null, 2));
  const [backendRawJson, setBackendRawJson] = useState<string | null>(null);
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

  useEffect(() => {
    setInputJson(JSON.stringify(input, null, 2));
    saveInput(input);
  }, [input]);

  const handleRunSimulation = async () => {
    if (!wasmExports) return;

    try {
      setError(null);
      const parsed = JSON.parse(inputJson) as Partial<SimulationInput>;
      const resolvedInput: SimulationInput = { ...defaultSimulationInput, ...parsed };
      setInput(resolvedInput);

      setJsResult(simulate1D(resolvedInput));
      const raw = wasmExports.runSimulation(JSON.stringify(resolvedInput));
      setBackendRawJson(raw);
      try {
        setBackendResult(JSON.parse(raw) as SimulationResult);
      } catch {
        setBackendResult(null);
      }
      setActivePane('results');
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const jsAltitudeSeries = toSeries(jsResult, (ts) => ts.altitude);
  const backendAltitudeSeries = toSeries(backendResult, (ts) => ts.altitude);
  const jsVelocitySeries = toSeries(jsResult, (ts) => ts.velocity);
  const backendVelocitySeries = toSeries(backendResult, (ts) => ts.velocity);
  const jsAccelerationSeries = toSeries(jsResult, (ts) => ts.acceleration);
  const backendAccelerationSeries = toSeries(backendResult, (ts) => ts.acceleration);
  const jsThrustSeries = toSeries(jsResult, (ts) => ts.thrustN);
  const backendThrustSeries = toSeries(backendResult, (ts) => ts.thrustN);
  const jsMassSeries = toSeries(jsResult, (ts) => ts.massKg);
  const backendMassSeries = toSeries(backendResult, (ts) => ts.massKg);
  const jsDragSeries = toSeries(jsResult, (ts) => ts.dragN);
  const backendDragSeries = toSeries(backendResult, (ts) => ts.dragN);

  return (
    <div className="App">
      <header className="AppHeader">
        <div className="AppHeader__title">
          <h1>OpenRocket Web Edition</h1>
          <div className="AppHeader__subtitle">Tablet-friendly prototype UI</div>
        </div>

        <div className="AppHeader__right">
          <div className="AppHeader__tabs" role="tablist" aria-label="Pane">
            <button
              type="button"
              className="TabButton"
              role="tab"
              aria-selected={activePane === 'inputs'}
              onClick={() => setActivePane('inputs')}
            >
              Inputs
            </button>
            <button
              type="button"
              className="TabButton"
              role="tab"
              aria-selected={activePane === 'results'}
              onClick={() => setActivePane('results')}
            >
              Results
            </button>
          </div>

          <div className="AppHeader__status">
            Backend: <strong>{wasmExports ? wasmExports.__kind : 'loading'}</strong>
          </div>

          <button className="Primary" onClick={handleRunSimulation} disabled={!wasmExports}>
            Run Simulation
          </button>
        </div>
      </header>

      {error && <p className="Error">{error}</p>}

      <div className="Layout" data-active-pane={activePane}>
        <section className="Panel Layout__inputs" aria-label="Inputs">
          <div className="PanelHeader">
            <strong>Inputs</strong>
            <div className="PanelHeader__right">
              <button
                type="button"
                onClick={() => setInputMode('form')}
                className={inputMode === 'form' ? 'PillButton Active' : 'PillButton'}
              >
                Form
              </button>
              <button
                type="button"
                onClick={() => setInputMode('json')}
                className={inputMode === 'json' ? 'PillButton Active' : 'PillButton'}
              >
                JSON
              </button>
            </div>
          </div>

          {inputMode === 'form' ? (
            <InputForm input={input} onChange={setInput} />
          ) : (
            <label className="FieldLabel">
              <span className="FieldLabelText">Simulation input (JSON)</span>
              <textarea value={inputJson} onChange={(e) => setInputJson(e.target.value)} rows={14} />
            </label>
          )}
        </section>

        <section className="Panel Layout__results" aria-label="Results">
          <strong>Results</strong>
          {!jsResult && (
            <p style={{ opacity: 0.8, marginTop: 8 }}>
              Run Simulation to see charts and summary.
            </p>
          )}

          {jsResult && (
            <div style={{ display: 'grid', gap: 12, marginTop: 10 }}>
              <SummaryGrid jsResult={jsResult} backendResult={backendResult} />

              <MultiLineChart
                title="Altitude vs Time (m)"
                series={[
                  jsAltitudeSeries && { name: 'JS', color: '#2563eb', points: jsAltitudeSeries },
                  backendAltitudeSeries && { name: 'Backend', color: '#f97316', points: backendAltitudeSeries }
                ].filter(Boolean) as ChartSeries[]}
              />

              <MultiLineChart
                title="Velocity vs Time (m/s)"
                series={[
                  jsVelocitySeries && { name: 'JS', color: '#2563eb', points: jsVelocitySeries },
                  backendVelocitySeries && { name: 'Backend', color: '#f97316', points: backendVelocitySeries }
                ].filter(Boolean) as ChartSeries[]}
              />

              <details>
                <summary>More charts</summary>
                <div style={{ display: 'grid', gap: 12, marginTop: 10 }}>
                  <MultiLineChart
                    title="Acceleration vs Time (m/s²)"
                    series={[
                      jsAccelerationSeries && { name: 'JS', color: '#2563eb', points: jsAccelerationSeries },
                      backendAccelerationSeries && { name: 'Backend', color: '#f97316', points: backendAccelerationSeries }
                    ].filter(Boolean) as ChartSeries[]}
                  />
                  <MultiLineChart
                    title="Thrust vs Time (N)"
                    series={[
                      jsThrustSeries && { name: 'JS', color: '#2563eb', points: jsThrustSeries },
                      backendThrustSeries && { name: 'Backend', color: '#f97316', points: backendThrustSeries }
                    ].filter(Boolean) as ChartSeries[]}
                  />
                  <MultiLineChart
                    title="Mass vs Time (kg)"
                    series={[
                      jsMassSeries && { name: 'JS', color: '#2563eb', points: jsMassSeries },
                      backendMassSeries && { name: 'Backend', color: '#f97316', points: backendMassSeries }
                    ].filter(Boolean) as ChartSeries[]}
                  />
                  <MultiLineChart
                    title="Drag vs Time (N)"
                    series={[
                      jsDragSeries && { name: 'JS', color: '#2563eb', points: jsDragSeries },
                      backendDragSeries && { name: 'Backend', color: '#f97316', points: backendDragSeries }
                    ].filter(Boolean) as ChartSeries[]}
                  />
                </div>
              </details>

              {backendRawJson && (
                <details>
                  <summary>Raw backend result JSON</summary>
                  <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12 }}>{backendRawJson}</pre>
                </details>
              )}
            </div>
          )}
        </section>
      </div>

      {!wasmExports && <p>WASM モジュールをロード中、もしくは未設定です。</p>}
    </div>
  );
}

function InputForm(props: { input: SimulationInput; onChange: (next: SimulationInput) => void }) {
  const { input, onChange } = props;
  const set = (key: keyof SimulationInput, value: number) => {
    onChange({ ...input, [key]: value });
  };

  return (
    <div className="FormGrid">
      <Field label="Dry mass (kg)" value={input.dryMassKg} step={0.01} min={0} onChange={(v) => set('dryMassKg', v)} />
      <Field
        label="Propellant mass (kg)"
        value={input.propellantMassKg}
        step={0.01}
        min={0}
        onChange={(v) => set('propellantMassKg', v)}
      />
      <Field label="Reference area (m²)" value={input.referenceAreaM2} step={0.0001} min={0} onChange={(v) => set('referenceAreaM2', v)} />
      <Field label="Drag coefficient (Cd)" value={input.dragCoefficient} step={0.01} min={0} onChange={(v) => set('dragCoefficient', v)} />
      <Field label="Average thrust (N)" value={input.averageThrustN} step={0.1} min={0} onChange={(v) => set('averageThrustN', v)} />
      <Field label="Burn time (s)" value={input.burnTimeS} step={0.1} min={0} onChange={(v) => set('burnTimeS', v)} />
      <Field label="Gravity (m/s²)" value={input.gravityMS2} step={0.0001} min={0} onChange={(v) => set('gravityMS2', v)} />
      <Field
        label="Sea-level air density (kg/m³)"
        value={input.airDensityKgM3}
        step={0.001}
        min={0}
        onChange={(v) => set('airDensityKgM3', v)}
      />
      <Field
        label="Atmosphere scale height (m)"
        value={input.atmosphereScaleHeightM}
        step={10}
        onChange={(v) => set('atmosphereScaleHeightM', v)}
      />
      <Field label="dt (s)" value={input.dtS} step={0.001} min={0.001} onChange={(v) => set('dtS', v)} />
      <Field label="Max time (s)" value={input.maxTimeS} step={1} min={1} onChange={(v) => set('maxTimeS', v)} />
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button type="button" onClick={() => onChange(defaultSimulationInput)}>
          Reset to default
        </button>
        <button type="button" onClick={() => onChange({ ...defaultSimulationInput, averageThrustN: 0 })}>
          No-thrust demo
        </button>
      </div>
    </div>
  );
}

function Field(props: {
  label: string;
  value: number;
  onChange: (value: number) => void;
  step?: number;
  min?: number;
}) {
  const { label, value, onChange, step, min } = props;
  return (
    <label className="FieldLabel">
      <span className="FieldLabelText">{label}</span>
      <input
        type="number"
        value={Number.isFinite(value) ? value : 0}
        step={step}
        min={min}
        onChange={(e) => onChange(e.target.value === '' ? 0 : Number(e.target.value))}
      />
    </label>
  );
}

function SummaryGrid(props: { jsResult: SimulationResult; backendResult: SimulationResult | null }) {
  const { jsResult, backendResult } = props;
  const diff =
    backendResult && Number.isFinite(backendResult.summary.maxAltitude)
      ? backendResult.summary.maxAltitude - jsResult.summary.maxAltitude
      : null;

  return (
    <div className="Summary">
      <div className="SummaryStats">
        <Stat
          label="Max altitude"
          value={`${jsResult.summary.maxAltitude.toFixed(1)} m`}
          sub={diff !== null ? `Δ backend ${diff.toFixed(2)} m` : undefined}
        />
        <Stat label="Max velocity" value={`${jsResult.summary.maxVelocity.toFixed(1)} m/s`} />
        <Stat label="Max accel" value={`${jsResult.summary.maxAcceleration.toFixed(1)} m/s²`} />
        <Stat label="Flight time" value={`${jsResult.summary.flightTime.toFixed(2)} s`} />
      </div>
      {backendResult && (
        <div style={{ opacity: 0.85, fontSize: 12 }}>
          Backend note: {backendResult.meta.note} (steps: {backendResult.meta.steps})
        </div>
      )}
    </div>
  );
}

function Stat(props: { label: string; value: string; sub?: string }) {
  const { label, value, sub } = props;
  return (
    <div className="StatCard">
      <div className="StatCard__label">{label}</div>
      <div className="StatCard__value">{value}</div>
      {sub && <div className="StatCard__sub">{sub}</div>}
    </div>
  );
}

type ChartSeries = { name: string; color: string; points: { x: number; y: number }[] };

function MultiLineChart(props: { series: ChartSeries[]; title: string }) {
  const { series, title } = props;
  const allPoints = series.flatMap((s) => s.points);
  if (allPoints.length < 2) return null;

  const width = 700;
  const height = 220;
  const padding = 30;

  const xs = allPoints.map((p) => p.x);
  const ys = allPoints.map((p) => p.y);
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);

  const rangeX = maxX - minX || 1;
  const rangeY = maxY - minY || 1;

  const toSvgX = (x: number) => padding + ((x - minX) / rangeX) * (width - padding * 2);
  const toSvgY = (y: number) =>
    height - padding - ((y - minY) / rangeY) * (height - padding * 2);

  return (
    <svg
      width="100%"
      viewBox={`0 0 ${width} ${height}`}
      className="Chart"
      role="img"
      aria-label={title}
    >
      <text x={padding} y={18} fontSize="12" fill="#333">
        {title}
      </text>
      <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="#aaa" />
      <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="#aaa" />
      {series.map((s) => {
        const polylinePoints = s.points
          .map((p) => `${toSvgX(p.x).toFixed(2)},${toSvgY(p.y).toFixed(2)}`)
          .join(' ');
        return (
          <polyline key={s.name} fill="none" stroke={s.color} strokeWidth="2" points={polylinePoints} />
        );
      })}
      <g transform={`translate(${width - padding - 4},${padding})`}>
        {series.map((s, idx) => (
          <g key={s.name} transform={`translate(0,${idx * 14})`}>
            <line x1={-46} y1={-4} x2={-34} y2={-4} stroke={s.color} strokeWidth="2" />
            <text x={0} y={0} fontSize="11" fill="#333" textAnchor="end">
              {s.name}
            </text>
          </g>
        ))}
      </g>
    </svg>
  );
}

function toSeries(
  result: SimulationResult | null,
  getY: (ts: SimulationResult['timeSeries']) => number[]
): { x: number; y: number }[] | null {
  if (!result) return null;
  const ts = result.timeSeries;
  const ys = getY(ts);
  return ts.time.map((t, i) => ({ x: t, y: ys[i] ?? 0 }));
}

export default App;
