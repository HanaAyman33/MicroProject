package tomasulo.core;

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

    public void stepOneCycle() {
        SimulationClock.nextCycle();

        if (br.hasResolvedBranch()) {
            if (br.shouldFlushQueue()) {
                issueUnit.jumpTo(br.getResolvedTargetPc());
            } else {
                issueUnit.jumpTo(br.getResolvedTargetPc());
            }
        }

        issueUnit.stepIssue(rs, mem, br, regStatus);
    }
}
