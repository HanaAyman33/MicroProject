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

    public ReservationStationEntry(String id, StationType type, String opcode, String destination) {
        this.id = id;
        this.type = type;
        this.opcode = opcode;
        this.destination = destination;
        this.executing = false;
    }

    public String getId() { return id; }
    public StationType getType() { return type; }
    public String getOpcode() { return opcode; }
    public String getDestination() { return destination; }

    public Object getVj() { return Vj; }
    public void setVj(Object vj) { Vj = vj; Qj = null; }

    public Object getVk() { return Vk; }
    public void setVk(Object vk) { Vk = vk; Qk = null; }

    public String getQj() { return Qj; }
    public void setQj(String qj) { Qj = qj; Vj = null; }

    public String getQk() { return Qk; }
    public void setQk(String qk) { Qk = qk; Vk = null; }

    public boolean isReady() {
        // For load/store semantics you might only need one operand; that check can be refined externally.
        return Qj == null && Qk == null && !executing;
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