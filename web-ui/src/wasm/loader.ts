export interface SimulationExports {
  // TeaVM がエクスポートする関数名に合わせる（ラッパーがある場合は調整）
  // ここでは簡易的に文字列ベースの呼び出しを想定
  runSimulation?: (ptr: number, len: number) => number;
  // TeaVM が生成する場合は別の形になることがある
  memory?: WebAssembly.Memory;
}

/**
 * WASM モジュールをロードし、エクスポートを返す（簡易版）
 */
export async function loadWasm(): Promise<any> {
  const response = await fetch('/wasm/app.wasm');
  const bytes = await response.arrayBuffer();
  const { instance } = await WebAssembly.instantiate(bytes, {
    env: {
      // 必要に応じてインポート関数を定義
    }
  });
  return instance.exports;
}
