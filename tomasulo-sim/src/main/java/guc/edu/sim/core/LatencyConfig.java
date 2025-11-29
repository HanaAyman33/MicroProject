package guc.edu.sim.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized latency configuration for execution units / instruction types.
 * Can be populated from GUI inputs before simulation starts.
 */
public class LatencyConfig {
    private final EnumMap<StationType, Integer> latencies;

    public LatencyConfig() {
        latencies = new EnumMap<>(StationType.class);
        // sensible defaults (can be overridden by GUI before simulation start)
        latencies.put(StationType.FP_ADD, 3);
        latencies.put(StationType.FP_MUL, 10);
        latencies.put(StationType.INTEGER, 1);
        latencies.put(StationType.LOAD, 2);
        latencies.put(StationType.STORE, 2);
    }

    public void setLatency(StationType type, int cycles) {
        if (cycles < 1) throw new IllegalArgumentException("latency must be >= 1");
        latencies.put(type, cycles);
    }

    public int getLatency(StationType type) {
        return latencies.getOrDefault(type, 1);
    }

    public Map<StationType, Integer> getAllLatencies() {
        return new EnumMap<>(latencies);
    }
}
