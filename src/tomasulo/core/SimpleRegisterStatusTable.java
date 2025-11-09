package tomasulo.core;
import java.util.HashMap;
import java.util.Map;

public class SimpleRegisterStatusTable implements RegisterStatusTable {

    private final Map<String, Boolean> busy = new HashMap<>();

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
    }
}
