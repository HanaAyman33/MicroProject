package guc.edu.sim.core;

import java.util.List;

/**
 * Thin orchestration wrapper to bind core simulation to the UI.
 * Provides simple no-op units for RS/Mem/Branch so we can
 * exercise Issue + Clock using the Program/IssueUnit pipeline.
 */
public class SimulatorState {

    private Program program;
    private IssueUnit issueUnit;
    private SimulationController controller;
    private SimpleRegisterStatusTable regStatus;

    private final ReservationStations rs = new NoOpReservationStations();
    private final MemoryUnitInterface mem = new NoOpMemoryUnit();
    private final BranchUnitInterface br = new NoOpBranchUnit();

    private int lastIssuedIndex = -1;

    public void loadProgramLines(List<String> lines) {
        ProgramLoader loader = new ProgramLoader();
        this.program = loader.loadFromLines(lines);
        this.issueUnit = new IssueUnit(program);
        this.regStatus = new SimpleRegisterStatusTable();
        this.controller = new SimulationController(issueUnit, rs, mem, br, regStatus);
        SimulationClock.reset();
        this.lastIssuedIndex = -1;
    }

    public boolean isProgramLoaded() {
        return program != null;
    }

    public boolean step() {
        if (controller == null || issueUnit == null) return false;
        int prevPc = issueUnit.getPc();
        // Advance one cycle; rely on IssueUnit PC movement to infer issue
        controller.stepOneCycle();
        boolean issued = issueUnit.getPc() > prevPc;
        lastIssuedIndex = issued ? prevPc : -1;
        return issued;
    }

    public void reset() {
        SimulationClock.reset();
        lastIssuedIndex = -1;
        if (issueUnit != null) issueUnit.jumpTo(0);
    }

    public int getCycle() { return SimulationClock.getCycle(); }
    public Program getProgram() { return program; }
    public int getLastIssuedIndex() { return lastIssuedIndex; }
    public IssueUnit getIssueUnit() { return issueUnit; }

    // --- Minimal no-op backend units to enable stepping ---
    static class NoOpReservationStations implements ReservationStations {
        @Override public boolean hasFreeFor(Instruction instr) { return true; }
        @Override public void accept(Instruction instr, RegisterStatusTable regStatus) { /* no-op */ }
    }
    static class NoOpMemoryUnit implements MemoryUnitInterface {
        @Override public boolean hasFreeFor(Instruction instr) { return true; }
        @Override public void accept(Instruction instr, RegisterStatusTable regStatus) { /* no-op */ }
    }
    static class NoOpBranchUnit implements BranchUnitInterface {
        @Override public boolean isFree() { return true; }
        @Override public void accept(Instruction instr, RegisterStatusTable regStatus) { /* no-op */ }
        @Override public boolean hasResolvedBranch() { return false; }
        @Override public int getResolvedTargetPc() { return 0; }
        @Override public boolean shouldFlushQueue() { return false; }
    }
}
