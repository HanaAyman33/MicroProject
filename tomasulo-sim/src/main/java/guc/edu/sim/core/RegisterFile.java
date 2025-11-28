package guc.edu.sim.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Architectural Register File:
 * - Holds the *committed* values only (no Qi here).
 * - Renaming / "Qi" for each register is handled by RegisterStatusTable.
 *
 * Usage with Tomasulo:
 * - When issuing an instruction:
 *      if regStatus.isBusy(srcReg) -> use its producer tag (Qj/Qk),
 *      else -> read() from this RegisterFile for source value (Vj/Vk).
 * - On write-back:
 *      only write() to the register if regStatus says that this tag
 *      is still the current producer for that destination.
 */
public class RegisterFile {
    private final Map<String, Object> regs = new HashMap<>();

    public RegisterFile() {
        // initialize 32 integer + 32 FP registers
        for (int i = 0; i < 32; i++) {
            regs.put("R" + i, 0L);
            regs.put("F" + i, 0.0);
        }
    }

    /**
     * Returns the committed value currently stored in this architectural register.
     * If the register is logically "busy", the issuing logic should NOT use this
     * value, but instead use the tag from the RegisterStatusTable.
     */
    public Object read(String name) {
        return regs.get(name);
    }

    /**
     * Write a committed value into the given register.
     * This should only be called when we are sure that:
     *   regStatus.getProducerTag(name) == broadcastTag
     * i.e., we are writing the most recent result for this register.
     */
    public void write(String name, Object value) {
        if (name == null) return;
        if (!regs.containsKey(name)) return;
        regs.put(name, value);
    }

    /**
     * Preload registers before simulation (manual or from test case).
     */
    public void preload(Map<String, Object> initialValues) {
        if (initialValues == null) return;
        for (Map.Entry<String, Object> e : initialValues.entrySet()) {
            String k = e.getKey();
            if (regs.containsKey(k)) regs.put(k, e.getValue());
        }
    }

    /**
     * Immutable snapshot for GUI display.
     */
    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(regs));
    }
}
