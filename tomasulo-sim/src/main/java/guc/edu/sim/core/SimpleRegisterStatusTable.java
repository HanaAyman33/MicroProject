package guc.edu.sim.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of a register status / renaming table.
 *
 * Conceptually this is the "Qi" field from Tomasulo slides, but external
 * to the RegisterFile:
 *   - producerTag[reg] = tag of RS/FU that will produce its next value.
 *   - If producerTag[reg] == null -> register is not waiting on any future write.
 */
public class SimpleRegisterStatusTable implements RegisterStatusTable {

    // True if some in-flight instruction will write this register.
    private final Map<String, Boolean> busy = new HashMap<>();

    // Tag of the reservation station / FU that will produce the reg's next value.
    private final Map<String, String> producerTag = new HashMap<>();

    /**
     * If you ever want to detect a pure WAW situation, you can use this.
     * Tomasulo with renaming *does not* block on WAW; this is just informational.
     */
    @Override
    public boolean causesIllegalWAW(Instruction instr) {
        String dest = instr.getDest();
        if (dest == null) return false;
        return isBusy(dest);
    }

    /**
     * For Tomasulo, register status usually does NOT cause structural hazards.
     * Structural hazards are handled by RS, LS buffers, etc.
     * So we return false here (no extra stall).
     */
    @Override
    public boolean causesStructuralProblem(Instruction instr) {
        return false;
    }

    /**
     * Mark that some instruction will write to this destination register.
     * This is used at issue time even before we know the exact producer tag.
     * Reservation stations can later call setProducerTag() once they have an ID.
     */
    public void markDestBusy(Instruction instr) {
        String dest = instr.getDest();
        if (dest != null) busy.put(dest, true);
    }

    /**
     * Mark a register as no longer waiting on a producer.
     * Usually called at commit/write-back when the tag for this register is done.
     */
    public void markDestFree(String reg) {
        if (reg == null) return;
        busy.put(reg, false);
        producerTag.remove(reg);
    }

    /**
     * Explicitly set the producer tag (Qi) for a register.
     * Called usually once the RS allocates an entry and knows its own tag.
     */
    public void setProducerTag(String reg, String tag) {
        if (reg == null || tag == null) return;
        busy.put(reg, true);
        producerTag.put(reg, tag);
    }

    /**
     * Return the tag of the RS/FU that will produce this register's value,
     * or null if it is not waiting on any in-flight instruction.
     */
    public String getProducerTag(String reg) {
        return producerTag.get(reg);
    }

    /**
     * Whether this register is waiting on some in-flight instruction.
     */
    public boolean isBusy(String reg) {
        if (reg == null) return false;
        // Derive from producerTag if present, else from busy map.
        if (producerTag.containsKey(reg)) return true;
        return busy.getOrDefault(reg, false);
    }

    /**
     * Used at write-back:
     * If the current producer tag for this register matches the CDB tag,
     * we clear it, marking the register as now "clean".
     */
    public void clearIfTagMatches(String reg, String tag) {
        if (reg == null || tag == null) return;
        String current = producerTag.get(reg);
        if (current != null && current.equals(tag)) {
            producerTag.remove(reg);
            busy.put(reg, false);
        }
    }
}
