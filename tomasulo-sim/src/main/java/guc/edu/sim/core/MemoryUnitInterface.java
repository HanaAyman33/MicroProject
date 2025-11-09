package guc.edu.sim.core;


public interface MemoryUnitInterface {
    boolean hasFreeFor(Instruction instr);
    void accept(Instruction instr, RegisterStatusTable regStatus);
}