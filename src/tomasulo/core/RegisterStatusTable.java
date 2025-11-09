package tomasulo.core;

public interface RegisterStatusTable {
    boolean causesIllegalWAW(Instruction instr);
    boolean causesStructuralProblem(Instruction instr);
}
