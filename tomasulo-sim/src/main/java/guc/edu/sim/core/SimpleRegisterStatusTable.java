package guc.edu.sim.core;

import java.util.HashMap;
import java.util.Map;

public class SimpleRegisterStatusTable implements RegisterStatusTable {

    private final Map<String, Boolean> busy = new HashMap<>();
    private final Map<String, String> producerTag = new HashMap<>();

    @Override
    public boolean causesIllegalWAW(Instruction instr) {
        String dest = instr.getDest();
        if (dest == null) return false;
        return busy.getOrDefault(dest, false);
    }

    @Override
    public boolean causesStructuralProblem(Instruction instr) {
        return false;
    }

    public void markDestBusy(Instruction instr) {
        String dest = instr.getDest();
        if (dest != null) busy.put(dest, true);
    }

    public void markDestFree(String reg) {
        busy.put(reg, false);
        producerTag.remove(reg);
    }

    public void setProducerTag(String reg, String tag) {
        if (reg == null || tag == null) return;
        busy.put(reg, true);
        producerTag.put(reg, tag);
    }

    public String getProducerTag(String reg) {
        return producerTag.get(reg);
    }

    public boolean isBusy(String reg) {
        return busy.getOrDefault(reg, false);
    }

    public void clearIfTagMatches(String reg, String tag) {
        String current = producerTag.get(reg);
        if (current != null && current.equals(tag)) {
            producerTag.remove(reg);
            busy.put(reg, false);
        }
    }
}