package guc.edu.sim.core;

import java.util.Optional;

/**
 * A single execution unit that can execute one reservation-station entry at a time.
 * Supports configurable latency and a simple tick() method to simulate cycles.
 */
public class ExecutionUnit {
    private final StationType unitType;
    private final LatencyConfig latencyConfig;
    private ReservationStationEntry current;
    private int remainingCycles;

    public ExecutionUnit(StationType unitType, LatencyConfig latencyConfig) {
        this.unitType = unitType;
        this.latencyConfig = latencyConfig;
    }

    public StationType getUnitType() { return unitType; }

    public boolean isIdle() {
        return current == null;
    }

    /**
     * Start executing an entry (assumes entry.isReady() true).
     */
    public boolean start(ReservationStationEntry entry) {
        if (!isIdle()) return false;
        if (entry == null) return false;
        if (!entry.isReady()) return false;
        current = entry;
        int latency = latencyConfig.getLatency(unitType);
        remainingCycles = Math.max(1, latency);
        entry.markExecuting();
        return true;
    }

    /**
     * Advance one cycle. If execution completes, return the completed entry.
     */
    public Optional<ReservationStationEntry> tick() {
        if (current == null) return Optional.empty();
        remainingCycles--;
        if (remainingCycles <= 0) {
            // compute result placeholder — integration point:
            // In your real simulator, replace computeResult(...) with real ALU behavior.
            Object res = computeResult(current);
            current.setResult(res);
            ReservationStationEntry finished = current;
            current = null;
            return Optional.of(finished);
        }
        return Optional.empty();
    }

    private Object computeResult(ReservationStationEntry entry) {
        double vj = entry.getVj() instanceof Number ? ((Number) entry.getVj()).doubleValue() : 0.0;
        double vk = entry.getVk() instanceof Number ? ((Number) entry.getVk()).doubleValue() : 0.0;
        return ALU.compute(entry.getOpcode(), vj, vk);
    }
}
