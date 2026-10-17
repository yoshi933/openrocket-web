package com.example.openrocket.browser;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.document.Simulation;
import info.openrocket.core.file.GeneralRocketLoader;
import info.openrocket.core.file.GeneralRocketSaver;
import info.openrocket.core.file.RocketLoadException;
import info.openrocket.core.startup.OpenRocketCore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class CoreBridge {
    private static final String DEFAULT_RESULT = "/app/engine/core-result.json";

    private CoreBridge() {
    }

    public static void main(String[] args) {
        String resultPath = System.getProperty("openrocket.result", DEFAULT_RESULT);
        try {
            OpenRocketCore.initialize();
            String result = execute(args);
            writeResult(resultPath, result);
            System.out.println(result);
        } catch (Exception e) {
            String result = jsonError(e);
            try {
                writeResult(resultPath, result);
            } catch (IOException writeError) {
                writeError.printStackTrace(System.err);
            }
            System.err.println(result);
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static String execute(String[] args) throws Exception {
        String operation = args.length == 0 ? "smoke" : args[0].toLowerCase(Locale.ROOT);
        switch (operation) {
            case "smoke":
                return inspectDocument(createDocument());
            case "inspect":
                requireArgs(args, 2, "inspect <input.ork>");
                return inspectDocument(loadDocument(Path.of(args[1])));
            case "roundtrip":
                requireArgs(args, 3, "roundtrip <input.ork> <output.ork>");
                OpenRocketDocument document = loadDocument(Path.of(args[1]));
                saveDocument(document, Path.of(args[2]));
                return "{\"ok\":true,\"operation\":\"roundtrip\",\"output\":\""
                        + escape(args[2]) + "\"}";
            case "simulate":
                requireArgs(args, 2, "simulate <input.ork> [simulation-index]");
                int simulationIndex = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                return simulate(loadDocument(Path.of(args[1])), simulationIndex);
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    private static OpenRocketDocument createDocument() {
        return info.openrocket.core.document.OpenRocketDocumentFactory.createNewRocket();
    }

    private static OpenRocketDocument loadDocument(Path path) throws IOException, RocketLoadException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Input .ork file does not exist: " + path);
        }
        return new GeneralRocketLoader(path.toFile()).load();
    }

    private static void saveDocument(OpenRocketDocument document, Path path) throws Exception {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        new GeneralRocketSaver().save(path.toFile(), document);
    }

    private static String inspectDocument(OpenRocketDocument document) {
        int componentCount = document.getSelectedConfiguration().getAllComponents().size();
        return "{\"ok\":true,\"operation\":\"inspect\",\"simulationCount\":"
                + document.getSimulationCount() + ",\"componentCount\":" + componentCount
                + ",\"rocketName\":\"" + escape(document.getRocket().getName()) + "\"}";
    }

    private static String simulate(OpenRocketDocument document, int simulationIndex) throws Exception {
        if (simulationIndex < 0 || simulationIndex >= document.getSimulationCount()) {
            throw new IllegalArgumentException("Simulation index out of range: " + simulationIndex);
        }
        Simulation simulation = document.getSimulation(simulationIndex);
        simulation.simulate();
        info.openrocket.core.simulation.FlightData data = simulation.getSimulatedData();
        return "{\"ok\":true,\"operation\":\"simulate\",\"simulationIndex\":" + simulationIndex
                + ",\"branchCount\":" + data.getBranchCount()
                + ",\"maxAltitude\":" + number(data.getMaxAltitude())
                + ",\"maxVelocity\":" + number(data.getMaxVelocity())
                + ",\"flightTime\":" + number(data.getFlightTime()) + "}";
    }

    private static String number(double value) {
        return Double.isFinite(value) ? Double.toString(value) : "null";
    }

    private static void requireArgs(String[] args, int count, String usage) {
        if (args.length < count) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static String jsonError(Exception e) {
        return "{\"ok\":false,\"error\":\"" + escape(e.getClass().getSimpleName())
                + "\",\"message\":\"" + escape(e.getMessage() == null ? "" : e.getMessage()) + "\"}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void writeResult(String resultPath, String result) throws IOException {
        Path path = Path.of(resultPath);
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, result + System.lineSeparator(), StandardCharsets.UTF_8);
    }

}
