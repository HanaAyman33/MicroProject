package guc.edu.sim.core;

import java.util.*;

/**
 * Test for memory dependency checking in Tomasulo's algorithm.
 * 
 * According to Tomasulo's algorithm, loads and stores accessing the same memory 
 * address must be handled with proper ordering constraints. An instruction 
 * shouldn't be issued unless the one preceding it (that accesses the same 
 * address) has written back.
 */
public class MemoryDependencyTest {

    public static void main(String[] args) {
        System.out.println("=== Memory Dependency Test ===\n");
        
        // Test case: Load followed by Store to the same address
        // The store should wait for the load to write back before issuing
        boolean test1 = testLoadStoreMemoryDependency();
        boolean test2 = testStoreStoreMemoryDependency();
        boolean test3 = testDifferentAddressNoConflict();
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("Test 1 (Load-Store same address): " + (test1 ? "PASS" : "FAIL"));
        System.out.println("Test 2 (Store-Store same address): " + (test2 ? "PASS" : "FAIL"));
        System.out.println("Test 3 (Different addresses - no conflict): " + (test3 ? "PASS" : "FAIL"));
        
        if (test1 && test2 && test3) {
            System.out.println("\nAll tests PASSED!");
            System.exit(0);
        } else {
            System.out.println("\nSome tests FAILED!");
            System.exit(1);
        }
    }
    
    /**
     * Test that a STORE instruction waits for a preceding LOAD to the same
     * address to complete write-back before issuing.
     * 
     * Program:
     * 0. L.D F0, 8(R1)     - Load from address 8 + R1
     * 1. MUL.D F4, F0, F2  - Multiply (depends on F0 from load)
     * 2. S.D F4, 8(R1)     - Store to same address (should wait for load WB)
     */
    private static boolean testLoadStoreMemoryDependency() {
        System.out.println("Test 1: Load-Store Memory Dependency");
        System.out.println("Expected: S.D should not issue until L.D completes write-back");
        System.out.println("---");
        
        // Create simulator
        SimulatorState sim = new SimulatorState();
        
        // Load program
        List<String> programLines = Arrays.asList(
            "L.D F0, 8(R1)",     // Instruction 0: Load from address 8+R1
            "MUL.D F4, F0, F2",  // Instruction 1: Multiply (depends on load)
            "S.D F4, 8(R1)"      // Instruction 2: Store to same address - should wait!
        );
        
        sim.loadProgramLines(programLines);
        
        // Set initial register values - R1 = 0, so address will be 8
        Map<String, Double> regValues = new HashMap<>();
        regValues.put("R1", 0.0);
        regValues.put("F2", 2.0);
        sim.loadInitialRegisterValues(regValues);
        
        // Set initial memory value at address 8
        Map<Integer, Double> memValues = new HashMap<>();
        memValues.put(8, 3.0);
        sim.loadInitialMemoryValues(memValues);
        
        // Configure simulator with specific latencies for predictable results
        sim.setConfigurationWithLatencies(3, 2, 2, 3, 3, 
                64, 16, 1, 10, 
                3, 10, 40, 1, 2, 2, 1);
        
        // Track issue cycles
        int loadIssueCycle = -1;
        int storeIssueCycle = -1;
        int loadWriteBackCycle = -1;
        
        // Run simulation until all instructions complete or max cycles reached
        int maxCycles = 50;
        for (int i = 0; i < maxCycles; i++) {
            sim.step();
            
            // Check instruction statuses
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.programIndex == 0 && status.issueCycle > 0) {
                    loadIssueCycle = status.issueCycle;
                    if (status.writeBackCycle > 0) {
                        loadWriteBackCycle = status.writeBackCycle;
                    }
                }
                if (status.programIndex == 2 && status.issueCycle > 0) {
                    storeIssueCycle = status.issueCycle;
                }
            }
            
            // Check if all instructions have completed write-back
            boolean allComplete = true;
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.writeBackCycle <= 0) {
                    allComplete = false;
                    break;
                }
            }
            
            if (allComplete) {
                break;
            }
        }
        
        // Output results
        System.out.println("Results:");
        System.out.println("  L.D F0, 8(R1) issued at cycle: " + loadIssueCycle);
        System.out.println("  L.D F0, 8(R1) write-back at cycle: " + loadWriteBackCycle);
        System.out.println("  S.D F4, 8(R1) issued at cycle: " + storeIssueCycle);
        
        // Verify the constraint
        boolean passed = (storeIssueCycle >= loadWriteBackCycle && loadWriteBackCycle > 0);
        System.out.println(passed ? "✓ PASS" : "✗ FAIL");
        System.out.println();
        return passed;
    }
    
    /**
     * Test that a STORE instruction waits for a preceding STORE to the same
     * address to complete write-back before issuing.
     * 
     * Program:
     * 0. S.D F0, 8(R1)     - Store to address 8 + R1
     * 1. ADD.D F4, F2, F2  - Some computation (to add cycles)
     * 2. S.D F4, 8(R1)     - Store to same address (should wait for first store WB)
     */
    private static boolean testStoreStoreMemoryDependency() {
        System.out.println("Test 2: Store-Store Memory Dependency");
        System.out.println("Expected: Second S.D should not issue until first S.D completes write-back");
        System.out.println("---");
        
        // Create simulator
        SimulatorState sim = new SimulatorState();
        
        // Load program
        List<String> programLines = Arrays.asList(
            "S.D F0, 8(R1)",     // Instruction 0: Store to address 8+R1
            "ADD.D F4, F2, F2",  // Instruction 1: Add (no dependency)
            "S.D F4, 8(R1)"      // Instruction 2: Store to same address - should wait!
        );
        
        sim.loadProgramLines(programLines);
        
        // Set initial register values
        Map<String, Double> regValues = new HashMap<>();
        regValues.put("R1", 0.0);
        regValues.put("F0", 1.0);
        regValues.put("F2", 2.0);
        sim.loadInitialRegisterValues(regValues);
        
        // Configure simulator
        sim.setConfigurationWithLatencies(3, 2, 2, 3, 3, 
                64, 16, 1, 10, 
                3, 10, 40, 1, 2, 2, 1);
        
        // Track issue cycles
        int store1IssueCycle = -1;
        int store2IssueCycle = -1;
        int store1WriteBackCycle = -1;
        
        // Run simulation
        int maxCycles = 50;
        for (int i = 0; i < maxCycles; i++) {
            sim.step();
            
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.programIndex == 0 && status.issueCycle > 0) {
                    store1IssueCycle = status.issueCycle;
                    if (status.writeBackCycle > 0) {
                        store1WriteBackCycle = status.writeBackCycle;
                    }
                }
                if (status.programIndex == 2 && status.issueCycle > 0) {
                    store2IssueCycle = status.issueCycle;
                }
            }
            
            boolean allComplete = true;
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.writeBackCycle <= 0) {
                    allComplete = false;
                    break;
                }
            }
            
            if (allComplete) break;
        }
        
        // Output results
        System.out.println("Results:");
        System.out.println("  First S.D issued at cycle: " + store1IssueCycle);
        System.out.println("  First S.D write-back at cycle: " + store1WriteBackCycle);
        System.out.println("  Second S.D issued at cycle: " + store2IssueCycle);
        
        boolean passed = (store2IssueCycle >= store1WriteBackCycle && store1WriteBackCycle > 0);
        System.out.println(passed ? "✓ PASS" : "✗ FAIL");
        System.out.println();
        return passed;
    }
    
    /**
     * Test that LOAD/STORE to different addresses can issue without waiting.
     * 
     * Program:
     * 0. L.D F0, 8(R1)     - Load from address 8 (R1=0)
     * 1. S.D F2, 16(R1)    - Store to address 16 (different address - should issue immediately)
     */
    private static boolean testDifferentAddressNoConflict() {
        System.out.println("Test 3: Different Addresses - No Conflict");
        System.out.println("Expected: S.D to different address should issue without waiting");
        System.out.println("---");
        
        // Create simulator
        SimulatorState sim = new SimulatorState();
        
        // Load program
        List<String> programLines = Arrays.asList(
            "L.D F0, 8(R1)",     // Instruction 0: Load from address 8
            "S.D F2, 16(R1)"     // Instruction 1: Store to address 16 (different)
        );
        
        sim.loadProgramLines(programLines);
        
        // Set initial register values
        Map<String, Double> regValues = new HashMap<>();
        regValues.put("R1", 0.0);
        regValues.put("F2", 5.0);
        sim.loadInitialRegisterValues(regValues);
        
        // Set initial memory values
        Map<Integer, Double> memValues = new HashMap<>();
        memValues.put(8, 3.0);
        sim.loadInitialMemoryValues(memValues);
        
        // Configure simulator
        sim.setConfigurationWithLatencies(3, 2, 2, 3, 3, 
                64, 16, 1, 10, 
                3, 10, 40, 1, 2, 2, 1);
        
        // Track issue cycles
        int loadIssueCycle = -1;
        int storeIssueCycle = -1;
        
        // Run simulation
        int maxCycles = 50;
        for (int i = 0; i < maxCycles; i++) {
            sim.step();
            
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.programIndex == 0 && status.issueCycle > 0) {
                    loadIssueCycle = status.issueCycle;
                }
                if (status.programIndex == 1 && status.issueCycle > 0) {
                    storeIssueCycle = status.issueCycle;
                }
            }
            
            boolean allComplete = true;
            for (SimulatorState.InstructionStatus status : statuses) {
                if (status.writeBackCycle <= 0) {
                    allComplete = false;
                    break;
                }
            }
            
            if (allComplete) break;
        }
        
        // Output results
        System.out.println("Results:");
        System.out.println("  L.D F0, 8(R1) issued at cycle: " + loadIssueCycle);
        System.out.println("  S.D F2, 16(R1) issued at cycle: " + storeIssueCycle);
        
        // Store should be able to issue without waiting for load write-back
        // since they access different addresses. We verify this by checking that 
        // the store doesn't wait for the load's write-back cycle.
        // The store should issue early (not waiting for load WB), but the exact
        // timing depends on structural hazards and other factors.
        
        // Get load write-back cycle for comparison
        int loadWriteBackCycle = -1;
        for (SimulatorState.InstructionStatus status : sim.getInstructionStatuses()) {
            if (status.programIndex == 0 && status.writeBackCycle > 0) {
                loadWriteBackCycle = status.writeBackCycle;
            }
        }
        
        // Verify the store issued BEFORE the load wrote back (proving no memory conflict delay)
        boolean passed = (storeIssueCycle > 0 && storeIssueCycle < loadWriteBackCycle);
        if (passed) {
            System.out.println("✓ PASS: Store issued before load write-back (no memory conflict delay)");
        } else {
            System.out.println("✗ FAIL: Store waited unnecessarily for load write-back");
        }
        System.out.println();
        return passed;
    }
}
