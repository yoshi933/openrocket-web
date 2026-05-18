import { defaultSimulationInput, runSimulationJson, type SimulationInput } from '../sim/sim';

export interface SimulationExports {
  runSimulation: (inputJson: string) => string;
  __kind: 'wasm' | 'stub';
}

function createStubExports(): SimulationExports {
  return {
    __kind: 'stub',
    runSimulation: (inputJson: string) => runSimulationJson(inputJson)
  };
}

type TeaVmRuntime = {
  wasm: {
    load: (path: string) => Promise<TeaVmInstance>;
  };
};

type TeaVmInstance = {
  memory: WebAssembly.Memory;
  instance: WebAssembly.Instance;
  stringData: (stringPtr: number) => number;
  arrayLength: (arrayPtr: number) => number;
  charArrayData: (arrayPtr: number) => number;
  catchException: () => number;
};

function getTeaVM(): TeaVmRuntime | null {
  const globalAny = globalThis as any;
  return globalAny?.TeaVM?.wasm ? (globalAny.TeaVM as TeaVmRuntime) : null;
}

function decodeJavaString(teavm: TeaVmInstance, stringPtr: number): string {
  if (!stringPtr) return '';
  const arrayPtr = teavm.stringData(stringPtr);
  const length = teavm.arrayLength(arrayPtr);
  const address = teavm.charArrayData(arrayPtr);
  const view = new Uint16Array(teavm.memory.buffer, address, length);
  let out = '';
  for (let i = 0; i < length; i++) {
    out += String.fromCharCode(view[i]);
  }
  return out;
}

function wrapExport<TArgs extends unknown[], TResult>(
  teavm: TeaVmInstance,
  fn: (...args: TArgs) => TResult
): (...args: TArgs) => TResult {
  return (...args: TArgs) => {
    const result = fn(...args);
    const ex = teavm.catchException();
    if (ex !== 0) {
      throw new Error('Java exception occurred in WASM');
    }
    return result;
  };
}

/**
 * WASM モジュールをロードし、エクスポートを返す（簡易版）
 */
export async function loadWasm(): Promise<SimulationExports> {
  try {
    const teavmRuntime = getTeaVM();
    if (!teavmRuntime) {
      return createStubExports();
    }

    const teavm = await teavmRuntime.wasm.load('/wasm/app.wasm');
    const maybeRunSimulationParams = (teavm.instance.exports as any).runSimulationParams as
      | ((
          dryMassKg: number,
          propellantMassKg: number,
          referenceAreaM2: number,
          dragCoefficient: number,
          averageThrustN: number,
          burnTimeS: number,
          gravityMS2: number,
          airDensityKgM3: number,
          atmosphereScaleHeightM: number,
          dtS: number,
          maxTimeS: number
        ) => number)
      | undefined;
    if (typeof maybeRunSimulationParams !== 'function') {
      return createStubExports();
    }

    return {
      __kind: 'wasm',
      runSimulation: (inputJson: string) => {
        try {
          const parsed = JSON.parse(inputJson) as Partial<SimulationInput>;
          const input: SimulationInput = { ...defaultSimulationInput, ...parsed };
          const runSimulationParams = wrapExport(teavm, maybeRunSimulationParams);
          const resultPtr = runSimulationParams(
            input.dryMassKg,
            input.propellantMassKg,
            input.referenceAreaM2,
            input.dragCoefficient,
            input.averageThrustN,
            input.burnTimeS,
            input.gravityMS2,
            input.airDensityKgM3,
            input.atmosphereScaleHeightM,
            input.dtS,
            input.maxTimeS
          );
          return decodeJavaString(teavm, resultPtr);
        } catch {
          return createStubExports().runSimulation(inputJson);
        }
      }
    };
  } catch {
    return createStubExports();
  }
}
