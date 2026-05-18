export type SimulationInput = {
  dryMassKg: number;
  propellantMassKg: number;
  referenceAreaM2: number;
  dragCoefficient: number;
  averageThrustN: number;
  burnTimeS: number;
  gravityMS2: number;
  /**
   * Sea-level air density (kg/m^3).
   * If `atmosphereScaleHeightM` is > 0, the simulator uses rho = rho0 * exp(-altitude / scaleHeight).
   */
  airDensityKgM3: number;
  /**
   * Atmosphere scale height (m). Set <= 0 to disable altitude-dependent density.
   */
  atmosphereScaleHeightM: number;
  dtS: number;
  maxTimeS: number;
};

export type SimulationResult = {
  meta: { backend: string; note: string; steps: number };
  summary: {
    maxAltitude: number;
    maxVelocity: number;
    maxAcceleration: number;
    flightTime: number;
    apogeeTime: number;
  };
  timeSeries: {
    time: number[];
    altitude: number[];
    velocity: number[];
    acceleration: number[];
    densityKgM3: number[];
    dragN: number[];
    massKg: number[];
    thrustN: number[];
  };
};

export const defaultSimulationInput: SimulationInput = {
  dryMassKg: 0.3,
  propellantMassKg: 0.05,
  referenceAreaM2: 0.003,
  dragCoefficient: 0.5,
  averageThrustN: 12,
  burnTimeS: 1.5,
  gravityMS2: 9.80665,
  airDensityKgM3: 1.225,
  atmosphereScaleHeightM: 8500,
  dtS: 0.02,
  maxTimeS: 60
};

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

export function simulate1D(input: SimulationInput): SimulationResult {
  const dryMassKg = input.dryMassKg > 0 ? input.dryMassKg : defaultSimulationInput.dryMassKg;
  const propellantMassKg = Math.max(0, input.propellantMassKg);
  const referenceAreaM2 =
    input.referenceAreaM2 > 0 ? input.referenceAreaM2 : defaultSimulationInput.referenceAreaM2;
  const dragCoefficient = Math.max(0, input.dragCoefficient);
  const averageThrustN = Math.max(0, input.averageThrustN);
  const burnTimeS = Math.max(0, input.burnTimeS);
  const gravityMS2 = input.gravityMS2 > 0 ? input.gravityMS2 : defaultSimulationInput.gravityMS2;
  const airDensityKgM3 = Math.max(0, input.airDensityKgM3);
  const atmosphereScaleHeightM =
    input.atmosphereScaleHeightM !== 0
      ? input.atmosphereScaleHeightM
      : defaultSimulationInput.atmosphereScaleHeightM;
  const dtS = input.dtS > 0 ? input.dtS : defaultSimulationInput.dtS;
  const maxTimeS = input.maxTimeS > 0 ? input.maxTimeS : defaultSimulationInput.maxTimeS;

  let maxSteps = Math.ceil(maxTimeS / dtS) + 1;
  maxSteps = clamp(maxSteps, 2, 20000);

  const time: number[] = [];
  const altitude: number[] = [];
  const velocity: number[] = [];
  const acceleration: number[] = [];
  const densityKgM3: number[] = [];
  const dragN: number[] = [];
  const massKg: number[] = [];
  const thrustN: number[] = [];

  let t = 0;
  let h = 0;
  let v = 0;

  let maxAltitude = 0;
  let maxVelocity = 0;
  let maxAcceleration = 0;
  let apogeeTime = 0;

  for (let i = 0; i < maxSteps; i++) {
    const burnFraction = burnTimeS > 0 ? clamp(t / burnTimeS, 0, 1) : 1;
    const remainingPropellant = propellantMassKg * (1 - burnFraction);
    const m = Math.max(dryMassKg, dryMassKg + remainingPropellant);
    const thrust = t <= burnTimeS ? averageThrustN : 0;
    const rho =
      atmosphereScaleHeightM > 0
        ? airDensityKgM3 * Math.exp(-Math.max(0, h) / atmosphereScaleHeightM)
        : airDensityKgM3;
    const drag = 0.5 * rho * dragCoefficient * referenceAreaM2 * v * Math.abs(v);
    const a = (thrust - drag - m * gravityMS2) / m;

    time.push(t);
    altitude.push(h);
    velocity.push(v);
    acceleration.push(a);
    densityKgM3.push(rho);
    dragN.push(drag);
    massKg.push(m);
    thrustN.push(thrust);

    if (h > maxAltitude) {
      maxAltitude = h;
      apogeeTime = t;
    }
    if (Math.abs(v) > maxVelocity) {
      maxVelocity = Math.abs(v);
    }
    if (Math.abs(a) > maxAcceleration) {
      maxAcceleration = Math.abs(a);
    }

    v = v + a * dtS;
    h = h + v * dtS;
    t = t + dtS;

    if (t > 0.5 && h <= 0 && v < 0) {
      break;
    }
  }

  return {
    meta: {
      backend: 'stub-js',
      note: '1D vertical toy simulator (not OpenRocket Core)',
      steps: time.length
    },
    summary: {
      maxAltitude,
      maxVelocity,
      maxAcceleration,
      flightTime: time.length > 0 ? time[time.length - 1] : 0,
      apogeeTime
    },
    timeSeries: { time, altitude, velocity, acceleration, densityKgM3, dragN, massKg, thrustN }
  };
}

export function runSimulationJson(inputJson: string): string {
  const parsed = JSON.parse(inputJson) as Partial<SimulationInput>;
  const input: SimulationInput = { ...defaultSimulationInput, ...parsed };
  const result = simulate1D(input);
  return JSON.stringify(result);
}
