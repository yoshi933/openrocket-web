import React, { useEffect, useState } from 'react';
import { loadWasm, type SimulationExports } from './wasm/loader';

function App() {
  const [wasmExports, setWasmExports] = useState<SimulationExports | null>(null);
  const [resultJson, setResultJson] = useState<string | null>(null);

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
      const orkData = '<sample ork data here>';
      const result = wasmExports.runSimulation(orkData);
      setResultJson(result);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div style={{ padding: 20 }}>
      <h1>OpenRocket Web Edition (UI)</h1>
      <p>
        Backend: {wasmExports ? wasmExports.__kind : 'loading'}
      </p>
      <button onClick={handleRunSimulation} disabled={!wasmExports}>
        Run Simulation
      </button>
      {resultJson && <pre style={{ whiteSpace: 'pre-wrap' }}>{resultJson}</pre>}
      {!wasmExports && <p>WASM モジュールをロード中、もしくは未設定です。</p>}
    </div>
  );
}

export default App;
