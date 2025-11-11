package guc.edu.sim.core;

/**
 * IssueUnit is responsible for issuing instructions from the program to the
 * Reservation Stations, Memory Unit, and Branch Unit.
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

        if (regStatus != null) {
            // Allow renaming: do not block on WAW at issue.
            if (regStatus.causesStructuralProblem(instr)) return false;
        }

        return true;
    }
    
    private void routeToUnit(Instruction instr,
                         ReservationStations rs,
                         MemoryUnitInterface mem,
                         BranchUnitInterface br,
                         RegisterStatusTable regStatus) {

    if (regStatus instanceof SimpleRegisterStatusTable s) {
        // RS implementation should set precise producer tag upon allocation.
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