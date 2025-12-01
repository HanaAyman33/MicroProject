package guc.edu.sim.core;

import java.util.Optional;

/**
 * A single execution unit that can execute one reservation-station entry at a time.
 * Supports configurable latency and a simple tick() method to simulate cycles.
 * 
 * FIXED: Latency now represents the NUMBER OF EXECUTION CYCLES
 * - Latency 1 = starts in cycle N, completes in cycle N+1
 * - Latency 2 = starts in cycle N, completes in cycle N+2
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

    public int getRemainingCycles() {
        return remainingCycles;
    }

    public ReservationStationEntry getCurrentEntry() {
        return current;
    }

    /**
     * Start executing an entry (assumes entry.isReady() true).
     * FIXED: Latency 1 means execution starts AND completes in the same cycle
     * - Latency 1: Issue cycle N → starts AND completes in cycle N+1 → WB in cycle N+2
     * - Latency 2: Issue cycle N → starts in cycle N+1 → completes in cycle N+2 → WB in cycle N+3
     */
    public boolean start(ReservationStationEntry entry) {
        if (!isIdle()) return false;
        if (entry == null) return false;
        if (!entry.isReady()) return false;
        
        current = entry;
        int latency = latencyConfig.getLatency(unitType, entry.getOpcode());
        
        // Latency represents how many cycles the instruction occupies an execution unit
        // Latency 0 is invalid, minimum is 1
        remainingCycles = Math.max(1, latency);
        
        entry.markExecuting();
        System.out.println("[ExecutionUnit-" + unitType + "] Started " + entry.getId() + 
                         " with latency " + remainingCycles + " cycles");
        return true;
    }

    /**
     * Advance one cycle. If execution completes, return the completed entry.
     * FIXED: For latency 1, instruction completes IMMEDIATELY (same cycle it started)
     * This is called AFTER the instruction starts, so latency 1 completes immediately.
     */
    public Optional<ReservationStationEntry> tick() {
        if (current == null) return Optional.empty();
        
        System.out.println("[ExecutionUnit-" + unitType + "] " + current.getId() + 
                         " executing... " + remainingCycles + " cycles remaining");
        
        // Decrement cycles
        remainingCycles--;
        
        // Check if execution is complete (reaches 0)
        if (remainingCycles == 0) {
            System.out.println("[ExecutionUnit-" + unitType + "] " + current.getId() + 
                             " completing execution");
            // Complete execution in this cycle
            Object res = computeResult(current);
            current.setResult(res);
            ReservationStationEntry finished = current;
            current = null;
            System.out.println("[ExecutionUnit-" + unitType + "] " + finished.getId() + " COMPLETED");
            return Optional.of(finished);
        }
        
        // Still executing
        return Optional.empty();
    }

    private Object computeResult(ReservationStationEntry entry) {
        double vj = entry.getVj() instanceof Number ? ((Number) entry.getVj()).doubleValue() : 0.0;
        double vk = entry.getVk() instanceof Number ? ((Number) entry.getVk()).doubleValue() : 0.0;
        return ALU.compute(entry.getOpcode(), vj, vk);
    }
}