export interface SimulationExports {
  runSimulation: (orkData: string) => string;
  __kind: 'wasm' | 'stub';
}

function createStubExports(): SimulationExports {
  return {
    __kind: 'stub',
    runSimulation: (orkData: string) =>
      JSON.stringify({
        summary: {
          maxAltitude: 1234.5,
          maxVelocity: 210.3,
          flightTime: 35.2
        },
        meta: {
          note: 'stub result (WASM not available)',
          orkDataLength: orkData.length
        }
      })
  };
}

/**
 * WASM モジュールをロードし、エクスポートを返す（簡易版）
 */
export async function loadWasm(): Promise<SimulationExports> {
  try {
    const response = await fetch('/wasm/app.wasm');
    if (!response.ok) {
      return createStubExports();
    }

    const bytes = await response.arrayBuffer();
    const { instance } = await WebAssembly.instantiate(bytes, {});

    const maybeRunSimulation = (instance.exports as any).runSimulation;
    if (typeof maybeRunSimulation !== 'function') {
      return createStubExports();
    }

    return {
      __kind: 'wasm',
      runSimulation: (orkData: string) => {
        try {
          const result = maybeRunSimulation(orkData);
          return typeof result === 'string' ? result : JSON.stringify(result);
        } catch {
          return createStubExports().runSimulation(orkData);
        }
      }
    };
  } catch {
    return createStubExports();
  }
}
