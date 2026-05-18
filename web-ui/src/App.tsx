import React, { useEffect, useState } from 'react';
import { loadWasm } from './wasm/loader';

function App() {
  const [wasmExports, setWasmExports] = useState<any | null>(null);
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

    const orkData = '<sample ork data here>';

    // TeaVM の JS ラッパがある場合はその通り呼び出す。
    // ここではダミーで結果を表示する。
    try {
      // 実際には文字列をメモリに書き込みポインタを渡す等の処理が必要
      const result = JSON.stringify({ message: 'stub: runSimulation not wired' });
      setResultJson(result);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div style={{ padding: 20 }}>
      <h1>OpenRocket Web Edition (UI)</h1>
      <button onClick={handleRunSimulation} disabled={!wasmExports}>Run Simulation</button>
      {resultJson && <pre style={{ whiteSpace: 'pre-wrap' }}>{resultJson}</pre>}
      {!wasmExports && <p>WASM モジュールをロード中、もしくは未設定です。</p>}
    </div>
  );
}

export default App;
