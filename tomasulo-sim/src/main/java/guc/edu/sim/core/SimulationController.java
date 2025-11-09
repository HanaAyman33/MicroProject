package guc.edu.sim.core;

public class SimulationController {

    private final IssueUnit issueUnit;
    private final ReservationStations rs;
    private final MemoryUnitInterface mem;
    private final BranchUnitInterface br;
    private final RegisterStatusTable regStatus;

    public SimulationController(IssueUnit issueUnit,
                                ReservationStations rs,
                                MemoryUnitInterface mem,
                                BranchUnitInterface br,
                                RegisterStatusTable regStatus) {
        this.issueUnit = issueUnit;
        this.rs = rs;
        this.mem = mem;
        this.br = br;
        this.regStatus = regStatus;
    }

    public boolean stepOneCycle() {
        SimulationClock.nextCycle();

        if (br != null && br.hasResolvedBranch()) {
            if (br.shouldFlushQueue()) {
                issueUnit.jumpTo(br.getResolvedTargetPc());
            } else {
                issueUnit.jumpTo(br.getResolvedTargetPc());
            }
        }

        return issueUnit.stepIssue(rs, mem, br, regStatus);
    }

    public IssueUnit getIssueUnit() {
        return issueUnit;
    }
}
