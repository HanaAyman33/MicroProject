package guc.edu.sim.core;

import java.util.*;

/**
 * Test for sequential issue timing per Tomasulo's algorithm.
 * This test verifies that instructions issue in consecutive cycles (1, 2, 3, 4, etc.)
 * when resources are available, and only stall when required resources are unavailable.
 */
public class SequentialIssueTest {
    
    public static void main(String[] args) {
        System.out.println("=== Sequential Issue Test ===\n");
        
        // Test case from problem statement:
        // L.D F6, 0(R2)    # Should issue cycle 1
        // L.D F2, 8(R2)    # Should issue cycle 2 (if buffer has space)
        // MUL.D F0, F2, F4 # Should issue cycle 3
        // SUB.D F8, F2, F6 # Should issue cycle 4
        // DIV.D F10, F0, F6 # Should issue cycle 5
        // ADD.D F6, F8, F2 # Should issue cycle 6
        // S.D F6, 8(R2)    # Should issue cycle 7
        
        SimulatorState sim = new SimulatorState();
        
        // Default configuration with plenty of resources
        sim.setConfigurationWithLatencies(
            3,  // fpAdd RS size
            2,  // fpMul RS size
            2,  // int RS size
            3,  // loadBufSize - enough space for both loads
            3,  // storeBufSize
            64, // cacheSize
            16, // blockSize
            1,  // cacheHitLatency
            10, // cacheMissPenalty
            3,  // fpAddLatency
            10, // fpMulLatency
            40, // fpDivLatency
            1,  // intLatency
            2,  // loadLatency
            2,  // storeLatency
            1   // branchLatency
        );
        
        List<String> lines = Arrays.asList(
            "L.D F6, 0(R2)",    // Should issue cycle 1
            "L.D F2, 8(R2)",    // Should issue cycle 2
            "MUL.D F0, F2, F4", // Should issue cycle 3
            "SUB.D F8, F2, F6", // Should issue cycle 4
            "DIV.D F10, F0, F6",// Should issue cycle 5
            "ADD.D F6, F8, F2", // Should issue cycle 6
            "S.D F6, 8(R2)"     // Should issue cycle 7
        );
        
        sim.loadProgramLines(lines);
        
        System.out.println("Running simulation for 100 cycles...\n");
        
        // Run enough cycles to complete all instructions
        for (int i = 0; i < 100; i++) {
            System.out.println("\n==================== CYCLE " + (i+1) + " ====================");
            boolean shouldContinue = sim.step();
            
            // Print current issue status
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            System.out.println("\nInstruction Status Summary:");
            for (int j = 0; j < statuses.size(); j++) {
                SimulatorState.InstructionStatus s = statuses.get(j);
                System.out.println("  [" + j + "] Issue=" + s.issueCycle + 
                                   " ExecStart=" + s.execStartCycle + 
                                   " ExecEnd=" + s.execEndCycle +
                                   " WriteBack=" + s.writeBackCycle);
            }
            
            // Check if all instructions have completed
            boolean allDone = true;
            for (SimulatorState.InstructionStatus s : statuses) {
                if (s.writeBackCycle <= 0) {
                    allDone = false;
                    break;
                }
            }
            
            if (!shouldContinue || allDone) {
                System.out.println("\n>>> Simulation complete <<<");
                break;
            }
        }
        
        // Verify expectations for sequential issue
        System.out.println("\n\n=== VERIFICATION (Sequential Issue) ===");
        List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
        
        boolean testPassed = true;
        
        // Expected issue cycles: 1, 2, 3, 4, 5, 6, 7
        int[] expectedIssueCycles = {1, 2, 3, 4, 5, 6, 7};
        String[] instrNames = {"L.D F6", "L.D F2", "MUL.D F0", "SUB.D F8", "DIV.D F10", "ADD.D F6", "S.D F6"};
        
        for (int i = 0; i < statuses.size() && i < expectedIssueCycles.length; i++) {
            int actualIssue = statuses.get(i).issueCycle;
            int expectedIssue = expectedIssueCycles[i];
            
            if (actualIssue == expectedIssue) {
                System.out.println("PASS: " + instrNames[i] + " issued in cycle " + actualIssue + " (expected " + expectedIssue + ")");
            } else {
                System.out.println("FAIL: " + instrNames[i] + " issued in cycle " + actualIssue + " (expected " + expectedIssue + ")");
                testPassed = false;
            }
        }
        
        // Verify all instructions completed
        boolean allCompleted = true;
        for (int j = 0; j < statuses.size(); j++) {
            SimulatorState.InstructionStatus s = statuses.get(j);
            if (s.writeBackCycle <= 0) {
                allCompleted = false;
                System.out.println("FAIL: " + instrNames[j] + " did not complete (writeBackCycle=" + s.writeBackCycle + ")");
                testPassed = false;
            }
        }
        
        if (allCompleted) {
            System.out.println("\nPASS: All instructions completed successfully");
        }
        
        System.out.println("\n=== TEST " + (testPassed ? "PASSED" : "FAILED") + " ===");
        
        // Exit with appropriate code for CI
        System.exit(testPassed ? 0 : 1);
    }
}
