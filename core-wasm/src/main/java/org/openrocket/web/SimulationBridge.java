package org.openrocket.web;

import org.teavm.interop.Export;

public class SimulationBridge {
    public static void main(String[] args) {
    }

    private static void appendNumber(StringBuilder sb, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            sb.append("null");
            return;
        }
        // TeaVM/WASM で Double#toString が不安定なケースがあるため、固定小数で出力する
        if (value < 0) {
            sb.append('-');
            value = -value;
        }
        long intPart = (long) value;
        sb.append(intPart);
        sb.append('.');
        double frac = value - intPart;
        for (int i = 0; i < 6; i++) {
            frac *= 10;
            int digit = (int) frac;
            if (digit < 0) digit = 0;
            if (digit > 9) digit = 9;
            sb.append((char) ('0' + digit));
            frac -= digit;
        }
    }

    /**
     * シミュレーション入力（JSON 文字列）を受け取り、結果を JSON 文字列で返す。
     *
     * 現時点では OpenRocket Core は同梱していないため、ブラウザでも動作する「最小の 1 次元(鉛直)」
     * フライトシミュレーションを実装している。入力 JSON の数値キー（例: dryMassKg 等）から必要値を抽出する。
     */
    @Export(name = "runSimulation")
    public static String runSimulation(String inputJson) {
        // NOTE: TeaVM WASM では JS から Java String を安全に渡すのが難しいため、
        // 現状は互換用に固定レスポンスを返す（UI 側は runSimulationParams を使用する）。
        return "{\"meta\":{\"backend\":\"wasm-java\",\"note\":\"use runSimulationParams\",\"steps\":0},\"summary\":{\"maxAltitude\":0.0,\"maxVelocity\":0.0,\"maxAcceleration\":0.0,\"flightTime\":0.0,\"apogeeTime\":0.0},\"timeSeries\":{\"time\":[0.0],\"altitude\":[0.0],\"velocity\":[0.0],\"acceleration\":[0.0],\"massKg\":[0.0],\"thrustN\":[0.0]}}";
    }

    @Export(name = "runSimulationParams")
    public static String runSimulationParams(
            double dryMassKg,
            double propellantMassKg,
            double referenceAreaM2,
            double dragCoefficient,
            double averageThrustN,
            double burnTimeS,
            double gravityMS2,
            double airDensityKgM3,
            double dtS,
            double maxTimeS) {
        Summary summary = simulate1D(
                dryMassKg,
                propellantMassKg,
                referenceAreaM2,
                dragCoefficient,
                averageThrustN,
                burnTimeS,
                gravityMS2,
                airDensityKgM3,
                dtS,
                maxTimeS);

        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        sb.append("\"meta\":{");
        sb.append("\"backend\":\"wasm-java\",");
        sb.append("\"note\":\"1D vertical toy simulator (params)\",");
        sb.append("\"steps\":").append(summary.steps);
        sb.append("},");

        sb.append("\"summary\":{");
        sb.append("\"maxAltitude\":");
        appendNumber(sb, summary.maxAltitude);
        sb.append(",\"maxVelocity\":");
        appendNumber(sb, summary.maxVelocity);
        sb.append(",\"maxAcceleration\":");
        appendNumber(sb, summary.maxAcceleration);
        sb.append(",\"flightTime\":");
        appendNumber(sb, summary.flightTime);
        sb.append(",\"apogeeTime\":");
        appendNumber(sb, summary.apogeeTime);
        sb.append("},");

        sb.append("\"timeSeries\":{");
        sb.append("\"time\":[0.0],\"altitude\":[0.0],\"velocity\":[0.0],\"acceleration\":[0.0],\"massKg\":[0.0],\"thrustN\":[0.0]");
        sb.append("}");

        sb.append('}');
        return sb.toString();
    }

    private static final class Summary {
        int steps;
        double maxAltitude;
        double maxVelocity;
        double maxAcceleration;
        double flightTime;
        double apogeeTime;
    }

    private static Summary simulate1D(
            double dryMassKg,
            double propellantMassKg,
            double referenceAreaM2,
            double dragCoefficient,
            double averageThrustN,
            double burnTimeS,
            double gravityMS2,
            double airDensityKgM3,
            double dtS,
            double maxTimeS) {
        if (dtS <= 0) dtS = 0.02;
        if (maxTimeS <= 0) maxTimeS = 60.0;
        if (burnTimeS < 0) burnTimeS = 0;
        if (dryMassKg <= 0) dryMassKg = 0.30;
        if (propellantMassKg < 0) propellantMassKg = 0;
        if (referenceAreaM2 <= 0) referenceAreaM2 = 0.003;
        if (dragCoefficient < 0) dragCoefficient = 0;
        if (airDensityKgM3 < 0) airDensityKgM3 = 0;
        if (gravityMS2 <= 0) gravityMS2 = 9.80665;

        int maxSteps = (int) (maxTimeS / dtS) + 2;
        if (maxSteps < 2) maxSteps = 2;
        if (maxSteps > 20000) maxSteps = 20000;

        double t = 0.0;
        double h = 0.0;
        double v = 0.0;

        Summary summary = new Summary();
        summary.maxAltitude = 0.0;
        summary.maxVelocity = 0.0;
        summary.maxAcceleration = 0.0;
        summary.apogeeTime = 0.0;

        int steps = 0;
        for (; steps < maxSteps; steps++) {
            double burnFraction = burnTimeS > 0 ? (t / burnTimeS) : 1.0;
            if (burnFraction < 0.0) burnFraction = 0.0;
            else if (burnFraction > 1.0) burnFraction = 1.0;

            double remainingPropellant = propellantMassKg * (1.0 - burnFraction);
            double m = dryMassKg + remainingPropellant;
            if (m <= 0) m = dryMassKg;

            double thrust = (t <= burnTimeS) ? averageThrustN : 0.0;
            double drag = 0.5 * airDensityKgM3 * dragCoefficient * referenceAreaM2 * v * v;
            if (v <= 0) drag = -drag;

            double a = (thrust - drag - m * gravityMS2) / m;

            if (h > summary.maxAltitude) {
                summary.maxAltitude = h;
                summary.apogeeTime = t;
            }
            double vAbs = v < 0 ? -v : v;
            if (vAbs > summary.maxVelocity) summary.maxVelocity = vAbs;
            double aAbs = a < 0 ? -a : a;
            if (aAbs > summary.maxAcceleration) summary.maxAcceleration = aAbs;

            v = v + a * dtS;
            h = h + v * dtS;
            t = t + dtS;

            if (t > 0.5 && h <= 0 && v < 0) {
                steps++;
                break;
            }
        }

        summary.steps = steps;
        summary.flightTime = steps > 0 ? (steps - 1) * dtS : 0.0;
        return summary;
    }
}
