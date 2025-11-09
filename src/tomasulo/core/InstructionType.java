package tomasulo.core;

public enum InstructionType {
    ALU_FP,     // FP ADD.D, SUB.D, MUL.D, DIV.D
    ALU_INT,    // integer ops like DADDI, DSUBI
    LOAD,       // L.D, LW, L.S
    STORE,      // S.D, SW
    BRANCH,     // BEQ, BNE
    UNKNOWN
}
