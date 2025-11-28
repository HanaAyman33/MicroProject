package guc.edu.sim.core;

/**
 * IssueUnit is responsible for issuing instructions from the program to the
 * Reservation Stations, Memory Unit, and Branch Unit, following Tomasulo:
 *
 * - In-order ISSUE (PC increases only when issue succeeds).
 * - Structural hazards checked through RS / Memory / Branch units.
 * - Data hazards handled via tags (RegisterStatusTable + ReservationStations).
 */
public class IssueUnit {

    private final Program program;
    private int pc = 0;

    public IssueUnit(Program program) {
        this.program = program;
    }

    public boolean hasNext() {
        return pc < program.size();
    }

    public int getPc() {
        return pc;
    }

    public void jumpTo(int newPc) {
        this.pc = newPc;
    }

    /**
     * Try to issue the instruction at the current PC.
     * If we cannot issue due to structural hazard (no free RS/LS/branch),
     * we return false and DO NOT increment the PC.
     * This enforces the rule: "once an instruction is stuck, we don't issue after it".
     */
    public boolean stepIssue(ReservationStations rs,
                             MemoryUnitInterface mem,
                             BranchUnitInterface br,
                             RegisterStatusTable regStatus) {
        if (!hasNext()) return false;

        Instruction instr = program.get(pc);

        if (!canIssue(instr, rs, mem, br, regStatus)) {
            return false;
        }

        routeToUnit(instr, rs, mem, br, regStatus);
        instr.setIssueCycle(SimulationClock.getCycle());
        pc++;
        return true;
    }

    /**
     * Check only for structural hazards (no free RS / LS / branch slot).
     * We do NOT block on WAW, since Tomasulo eliminates WAW using renaming.
     */
    private boolean canIssue(Instruction instr,
                             ReservationStations rs,
                             MemoryUnitInterface mem,
                             BranchUnitInterface br,
                             RegisterStatusTable regStatus) {

        switch (instr.getType()) {
            case ALU_FP:
            case ALU_INT:
                if (!rs.hasFreeFor(instr)) return false;
                break;
            case LOAD:
            case STORE:
                if (!mem.hasFreeFor(instr)) return false;
                break;
            case BRANCH:
                if (!br.isFree()) return false;
                break;
            default:
                return false;
        }

        // If you want to add extra structural constraints via regStatus,
        // keep them here. We currently assume no additional structural limits.
        if (regStatus != null) {
            if (regStatus.causesStructuralProblem(instr)) return false;
        }

        return true;
    }

    /**
     * Route the instruction to its corresponding part of the Tomasulo architecture:
     * - RS for ALU (FP/INT)
     * - Memory Unit for loads/stores
     * - Branch unit for branches
     *
     * We also inform the register-status table that the destination register
     * will be produced by some in-flight instruction. The precise tag is typically
     * set by the RS once it allocates an entry (via setProducerTag()).
     */
    private void routeToUnit(Instruction instr,
                             ReservationStations rs,
                             MemoryUnitInterface mem,
                             BranchUnitInterface br,
                             RegisterStatusTable regStatus) {

        if (regStatus instanceof SimpleRegisterStatusTable s) {
            // Mark that the destination register is going to be written.
            // The exact producer tag will be set by the RS when it knows its ID.
            s.markDestBusy(instr);
        }

        switch (instr.getType()) {
            case ALU_FP:
            case ALU_INT:
                rs.accept(instr, regStatus);
                break;
            case LOAD:
            case STORE:
                mem.accept(instr, regStatus);
                break;
            case BRANCH:
                br.accept(instr, regStatus);
                break;
            default:
                break;
        }
    }
}
