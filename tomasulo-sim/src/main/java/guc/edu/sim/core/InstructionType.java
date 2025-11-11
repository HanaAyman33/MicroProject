package guc.edu.sim.core;

/**
 * Enum representing instruction categories used by the simulator.
 * Matches usage in IssueUnit, ProgramLoader, and UI controller.
 */
public enum InstructionType {
    LOAD,
    STORE,
    BRANCH,
    ALU_FP,
    ALU_INT,
    UNKNOWN
}
