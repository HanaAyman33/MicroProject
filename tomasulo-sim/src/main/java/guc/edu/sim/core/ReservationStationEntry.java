package guc.edu.sim.core;

/**
 * Minimal representation of a reservation-station entry (an instruction in a station).
 * Designed to be compatible with typical Tomasulo implementations using Vj/Vk and Qj/Qk.
 *
 * Note: Replace Object with the concrete data type your simulator uses for values (Integer, Long, Double).
 */
public class ReservationStationEntry {
    private final String id;              // unique tag, e.g., "RS1", "RS2" or the ROB tag
    private final StationType type;
    private final String opcode;          // e.g., "ADD.D", "LW", etc.
    private Object Vj;                    // value of source j if ready
    private Object Vk;                    // value of source k if ready
    private String Qj;                    // tag of producer for j if not ready
    private String Qk;                    // tag of producer for k if not ready
    private String destination;           // register or ROB tag to write result to
    private boolean executing;            // execution started (on an execution unit)
    private Object result;                // computed result after execution
    private int readyCycle = -1;          // cycle when this entry became ready (-1 means never/not yet)

    public ReservationStationEntry(String id, StationType type, String opcode, String destination) {
        this.id = id;
        this.type = type;
        this.opcode = opcode;
        this.destination = destination;
        this.executing = false;
        this.readyCycle = -1;
    }

    public String getId() { return id; }
    public StationType getType() { return type; }
    public String getOpcode() { return opcode; }
    public String getDestination() { return destination; }

    public Object getVj() { return Vj; }
    public void setVj(Object vj) { Vj = vj; Qj = null; }
    
    /** Set Vj value and record readyCycle if this made the entry ready (from CDB broadcast) */
    public void setVj(Object vj, int currentCycle) { 
        Vj = vj; 
        Qj = null; 
        updateReadyCycle(currentCycle);
    }

    public Object getVk() { return Vk; }
    public void setVk(Object vk) { Vk = vk; Qk = null; }
    
    /** Set Vk value and record readyCycle if this made the entry ready (from CDB broadcast) */
    public void setVk(Object vk, int currentCycle) { 
        Vk = vk; 
        Qk = null; 
        updateReadyCycle(currentCycle);
    }

    public String getQj() { return Qj; }
    public void setQj(String qj) { Qj = qj; Vj = null; }

    public String getQk() { return Qk; }
    public void setQk(String qk) { Qk = qk; Vk = null; }
    
    /** Update readyCycle if all operands are now available */
    private void updateReadyCycle(int currentCycle) {
        if (Qj == null && Qk == null && readyCycle < 0) {
            readyCycle = currentCycle;
        }
    }
    
    /** Get the cycle when this entry became ready */
    public int getReadyCycle() { return readyCycle; }
    
    /** Set the readyCycle (used when entry is allocated with all operands ready) */
    public void setReadyCycle(int cycle) { this.readyCycle = cycle; }

    public boolean isReady() {
        // For load/store semantics you might only need one operand; that check can be refined externally.
        return Qj == null && Qk == null && !executing;
    }
    
    /**
     * Check if the entry is ready to be dispatched in the given cycle.
     * An entry is ready for dispatch only if:
     * 1. All operands are available (Qj == null && Qk == null)
     * 2. It's not already executing
     * 3. Either it was issued with all operands ready (readyCycle < 0 or readyCycle < currentCycle)
     *    OR it received operands from CDB in a previous cycle (currentCycle > readyCycle)
     */
    public boolean isReadyForDispatch(int currentCycle) {
        if (Qj != null || Qk != null || executing) {
            return false;
        }
        // If readyCycle < 0, operands were ready at issue time (not from CDB)
        // If readyCycle > 0, operands came from CDB, so must wait until next cycle
        return readyCycle < 0 || currentCycle > readyCycle;
    }

    public boolean isExecuting() { return executing; }
    public void markExecuting() { executing = true; }

    public void setResult(Object result) { this.result = result; }
    public Object getResult() { return result; }

    @Override
    public String toString() {
        return String.format("%s(%s) op=%s Vj=%s Vk=%s Qj=%s Qk=%s dest=%s exec=%b",
                id, type, opcode, Vj, Vk, Qj, Qk, destination, executing);
    }
}