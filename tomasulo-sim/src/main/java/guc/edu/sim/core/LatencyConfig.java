package guc.edu.sim.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized latency configuration for execution units / instruction types.
 * Can be populated from GUI inputs before simulation starts.
 */
public class LatencyConfig {
    private final EnumMap<StationType, Integer> latencies;
    private int fpDivLatency;

    public LatencyConfig() {
        latencies = new EnumMap<>(StationType.class);
        // sensible defaults (can be overridden by GUI before simulation start)
        latencies.put(StationType.FP_ADD, 3);
        latencies.put(StationType.FP_MUL, 10);
        latencies.put(StationType.INTEGER, 1);
        latencies.put(StationType.LOAD, 2);
        latencies.put(StationType.STORE, 2);
        fpDivLatency = 40;
    }

    public void setLatency(StationType type, int cycles) {
        if (cycles < 1) throw new IllegalArgumentException("latency must be >= 1");
        latencies.put(type, cycles);
    }

    public void setDivisionLatency(int cycles) {
        if (cycles < 1) throw new IllegalArgumentException("latency must be >= 1");
        fpDivLatency = cycles;
    }

    public int getLatency(StationType type) {
        return latencies.getOrDefault(type, 1);
    }

    public int getLatency(StationType type, String opcode) {
        if (type == StationType.FP_MUL && isDivisionOpcode(opcode)) {
            return fpDivLatency;
        }
        return getLatency(type);
    }

    public Map<StationType, Integer> getAllLatencies() {
        return new EnumMap<>(latencies);
    }

    private boolean isDivisionOpcode(String opcode) {
        if (opcode == null) return false;
        String op = opcode.toUpperCase();
        if (op.endsWith(".D") || op.endsWith(".S")) {
            op = op.substring(0, op.length() - 2);
        }
        return op.contains("DIV");
    }
}
