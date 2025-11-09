package tomasulo.core;

public interface BranchUnitInterface {
    boolean isFree();
    void accept(Instruction instr, RegisterStatusTable regStatus);

    boolean hasResolvedBranch();
    int getResolvedTargetPc();
    boolean shouldFlushQueue();
}
