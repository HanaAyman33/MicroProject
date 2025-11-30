package guc.edu.sim.core;

/**
 * Types of reservation stations / execution units used in the Tomasulo simulator.
 * Matches common categories: FP Add/Sub, FP Mul/Div, Integer, Load, Store.
 */
public enum StationType {
    FP_ADD,    // Floating point add / sub
    FP_MUL,    // Floating point mul / div
    INTEGER,   // Integer ALU (ADDI, SUBI, etc)
    LOAD,      // Load (LW, LD, L.D, L.S)
    STORE ,     // Store (SW, SD, S.D, S.S)
}