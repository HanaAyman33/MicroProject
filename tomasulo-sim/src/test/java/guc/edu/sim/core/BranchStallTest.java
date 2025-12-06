package guc.edu.sim.core;

import java.util.*;

/**
 * Test for branch stalling in Tomasulo's algorithm.
 * 
 * According to Tomasulo's algorithm WITHOUT branch prediction, instructions
 * following a branch instruction should be stalled and only issued when the
 * branch writes back (completes execution).
 */
public class BranchStallTest {

    public static void main(String[] args) {
        System.out.println("=== Branch Stall Test ===\n");
        
        boolean test1 = testBranchStallsSubsequentInstruction();
        boolean test2 = testBranchStallsMultipleInstructions();
        boolean test3 = testInstructionIssuesOnBranchWriteback();
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("Test 1 (Branch stalls subsequent instruction): " + (test1 ? "PASS" : "FAIL"));
        System.out.println("Test 2 (Branch stalls multiple instructions): " + (test2 ? "PASS" : "FAIL"));
        System.out.println("Test 3 (Instruction issues on branch writeback): " + (test3 ? "PASS" : "FAIL"));
        
        if (test1 && test2 && test3) {
            System.out.println("\nAll tests PASSED!");
            System.exit(0);
        } else {
            System.out.println("\nSome tests FAILED!");
            System.exit(1);
        }
    }
    
    /**
     * Test that a LOAD instruction following a branch is stalled until
     * the branch writes back.
     * 
     * Program:
     * 0. LOOP: L.D F0, 8(R1)      - Load
     * 1. ADD.D F2, F0, F4          - Add (depends on load)
     * 2. BNE R1, R2, LOOP          - Branch (conditional)
     * 
     * After the first iteration, when the branch is taken:
     * - The L.D should not issue until the BNE writes back
     */
    private static boolean testBranchStallsSubsequentInstruction() {
        System.out.println("Test 1: Branch Stalls Subsequent Instruction");
        System.out.println("Expected: L.D should not issue until BNE writes back");
        System.out.println("---");
        
        // Create simulator
        SimulatorState sim = new SimulatorState();
        
        // Load program with a simple loop
        List<String> programLines = Arrays.asList(
            "LOOP: L.D F0, 8(R1)",    // Instruction 0: Load
            "ADD.D F2, F0, F4",        // Instruction 1: Add (depends on load)
            "BNE R1, R2, LOOP"         // Instruction 2: Branch to LOOP if R1 != R2
        );
        
        sim.loadProgramLines(programLines);
        
        // Set initial register values
        // R1 != R2 to ensure branch is taken
        Map<String, Double> regValues = new HashMap<>();
        regValues.put("R1", 0.0);
        regValues.put("R2", 1.0);  // R1 != R2, so branch will be taken
        regValues.put("F4", 2.0);
        sim.loadInitialRegisterValues(regValues);
        
        // Set initial memory value at address 8
        Map<Integer, Double> memValues = new HashMap<>();
        memValues.put(8, 3.0);
        sim.loadInitialMemoryValues(memValues);
        
        // Configure simulator with specific latencies for predictable results
        // fpAdd=3, fpMul=2, int=2, loadBuf=3, storeBuf=3, cache=64, block=16, hit=1, miss=10
        // fpAddLat=3, fpMulLat=10, fpDivLat=40, intLat=1, loadLat=2, storeLat=2, branchLat=1
        sim.setConfigurationWithLatencies(3, 2, 2, 3, 3, 
                64, 16, 1, 10, 
                3, 10, 40, 1, 2, 2, 1);
        
        // Track issue and writeback cycles
        int branchIssueCycle = -1;
        int branchWriteBackCycle = -1;
        int secondLoadIssueCycle = -1;  // Second iteration's load
        
        // Run simulation for enough cycles to see two iterations
        int maxCycles = 50;
        for (int i = 0; i < maxCycles; i++) {
            sim.step();
            
            // Check instruction statuses
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            
            for (SimulatorState.InstructionStatus status : statuses) {
                // First iteration branch (programIndex=2, iteration=1)
                if (status.programIndex == 2 && status.iteration == 1) {
                    if (status.issueCycle > 0 && branchIssueCycle < 0) {
                        branchIssueCycle = status.issueCycle;
                    }
                    if (status.writeBackCycle > 0 && branchWriteBackCycle < 0) {
                        branchWriteBackCycle = status.writeBackCycle;
                    }
                }
                
                // Second iteration load (programIndex=0, iteration=2)
                if (status.programIndex == 0 && status.iteration == 2) {
                    if (status.issueCycle > 0 && secondLoadIssueCycle < 0) {
                        secondLoadIssueCycle = status.issueCycle;
                    }
                }
            }
            
            // Stop once we have both measurements
            if (branchWriteBackCycle > 0 && secondLoadIssueCycle > 0) {
                break;
            }
        }
        
        // Output results
        System.out.println("Results:");
        System.out.println("  BNE issued at cycle: " + branchIssueCycle);
        System.out.println("  BNE write-back at cycle: " + branchWriteBackCycle);
        System.out.println("  Second L.D issued at cycle: " + secondLoadIssueCycle);
        
        // Verify the constraint: L.D should issue at or after branch writeback
        boolean passed = (secondLoadIssueCycle >= branchWriteBackCycle && branchWriteBackCycle > 0);
        System.out.println(passed ? "✓ PASS" : "✗ FAIL");
        if (!passed) {
            System.out.println("  ERROR: L.D issued BEFORE branch write-back!");
        }
        System.out.println();
        return passed;
    }
    
    /**
     * Test that multiple instructions following a branch are all stalled.
     */
    private static boolean testBranchStallsMultipleInstructions() {
        System.out.println("Test 2: Branch Stalls Multiple Instructions");
        System.out.println("Expected: Instructions after branch should not issue until branch writes back");
        System.out.println("---");
        
        // Create simulator
        SimulatorState sim = new SimulatorState();
        
        // Load program
        List<String> programLines = Arrays.asList(
            "DADDI R3, R1, 1",         // Instruction 0: Integer add
            "BEQ R1, R2, SKIP",        // Instruction 1: Branch
            "DADDI R4, R1, 2",         // Instruction 2: After branch (should be stalled)
            "SKIP: DADDI R5, R1, 3"    // Instruction 3: After branch (should be stalled if branch not taken)
        );
        
        sim.loadProgramLines(programLines);
        
        // Set initial register values
        // R1 != R2 to ensure branch is NOT taken (fall through)
        Map<String, Double> regValues = new HashMap<>();
        regValues.put("R1", 1.0);
        regValues.put("R2", 2.0);  // R1 != R2, so branch NOT taken
        sim.loadInitialRegisterValues(regValues);
        
        // Configure simulator
        sim.setConfigurationWithLatencies(3, 2, 2, 3, 3, 
                64, 16, 1, 10, 
                3, 10, 40, 1, 2, 2, 1);
        
        // Track cycles
        int branchIssueCycle = -1;
        int branchWriteBackCycle = -1;
        int nextInstrIssueCycle = -1;  // Instruction after branch
        
        // Run simulation
        int maxCycles = 30;
        for (int i = 0; i < maxCycles; i++) {
            sim.step();
            
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.programIndex == 1 && status.iteration == 1) {  // Branch
                    if (status.issueCycle > 0 && branchIssueCycle < 0) {
                        branchIssueCycle = status.issueCycle;
                    }
                    if (status.writeBackCycle > 0 && branchWriteBackCycle < 0) {
                        branchWriteBackCycle = status.writeBackCycle;
                    }
                }
                if (status.programIndex == 2 && status.iteration == 1) {  // Instruction after branch
                    if (status.issueCycle > 0 && nextInstrIssueCycle < 0) {
                        nextInstrIssueCycle = status.issueCycle;
                    }
                }
            }
            
            if (branchWriteBackCycle > 0 && nextInstrIssueCycle > 0) {
                break;
            }
        }
        
        // Output results
        System.out.println("Results:");
        System.out.println("  BEQ issued at cycle: " + branchIssueCycle);
        System.out.println("  BEQ write-back at cycle: " + branchWriteBackCycle);
        System.out.println("  DADDI R4 (after branch) issued at cycle: " + nextInstrIssueCycle);
        
        // Verify
        boolean passed = (nextInstrIssueCycle >= branchWriteBackCycle && branchWriteBackCycle > 0);
        System.out.println(passed ? "✓ PASS" : "✗ FAIL");
        if (!passed) {
            System.out.println("  ERROR: Instruction after branch issued BEFORE branch write-back!");
        }
        System.out.println();
        return passed;
    }
    
    /**
     * Test that an instruction can issue in the SAME cycle as the branch writes back.
     */
    private static boolean testInstructionIssuesOnBranchWriteback() {
        System.out.println("Test 3: Instruction Issues on Branch Writeback Cycle");
        System.out.println("Expected: Instruction should be able to issue in the same cycle as branch writeback");
        System.out.println("---");
        
        // Create simulator
        SimulatorState sim = new SimulatorState();
        
        // Load program
        List<String> programLines = Arrays.asList(
            "DADDI R3, R1, 1",         // Instruction 0: Integer add
            "BEQ R1, R2, SKIP",        // Instruction 1: Branch
            "DADDI R4, R1, 2",         // Instruction 2: Should issue when branch writes back
            "SKIP: DADDI R5, R1, 3"    // Instruction 3
        );
        
        sim.loadProgramLines(programLines);
        
        // Set initial register values
        // R1 != R2 to ensure branch is NOT taken
        Map<String, Double> regValues = new HashMap<>();
        regValues.put("R1", 1.0);
        regValues.put("R2", 2.0);
        sim.loadInitialRegisterValues(regValues);
        
        // Configure simulator with branch latency = 1 for faster test
        sim.setConfigurationWithLatencies(3, 2, 2, 3, 3, 
                64, 16, 1, 10, 
                3, 10, 40, 1, 2, 2, 1);
        
        // Track cycles
        int branchWriteBackCycle = -1;
        int nextInstrIssueCycle = -1;
        
        // Run simulation
        int maxCycles = 30;
        for (int i = 0; i < maxCycles; i++) {
            sim.step();
            
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.programIndex == 1 && status.iteration == 1) {
                    if (status.writeBackCycle > 0 && branchWriteBackCycle < 0) {
                        branchWriteBackCycle = status.writeBackCycle;
                    }
                }
                if (status.programIndex == 2 && status.iteration == 1) {
                    if (status.issueCycle > 0 && nextInstrIssueCycle < 0) {
                        nextInstrIssueCycle = status.issueCycle;
                    }
                }
            }
            
            if (branchWriteBackCycle > 0 && nextInstrIssueCycle > 0) {
                break;
            }
        }
        
        // Output results
        System.out.println("Results:");
        System.out.println("  BEQ write-back at cycle: " + branchWriteBackCycle);
        System.out.println("  DADDI R4 issued at cycle: " + nextInstrIssueCycle);
        
        // Verify: Instruction should issue at exactly the same cycle as branch writeback
        // (or possibly later if there are structural hazards)
        boolean passed = (nextInstrIssueCycle == branchWriteBackCycle);
        System.out.println(passed ? "✓ PASS" : "✗ FAIL");
        if (!passed) {
            if (nextInstrIssueCycle > branchWriteBackCycle) {
                System.out.println("  NOTE: Instruction issued AFTER branch write-back (may be due to other hazards)");
            } else {
                System.out.println("  ERROR: Instruction issued BEFORE branch write-back!");
            }
        }
        System.out.println();
        return passed;
    }
}
