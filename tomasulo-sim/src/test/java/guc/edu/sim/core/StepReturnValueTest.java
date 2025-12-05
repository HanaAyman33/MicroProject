package guc.edu.sim.core;

import java.util.*;

/**
 * Test for step() return value with in-order issue.
 * Verifies that step() returns true when the simulation should continue,
 * even when no instruction is issued in a cycle (e.g., instructions in-flight
 * or stalled waiting for resources).
 */
public class StepReturnValueTest {
    
    public static void main(String[] args) {
        System.out.println("=== Step Return Value Test (In-Order Issue) ===\n");
        
        SimulatorState sim = new SimulatorState();
        
        // Set up configuration with only 1 load buffer slot to force stall
        sim.setConfigurationWithLatencies(
            3,  // fpAdd RS size
            2,  // fpMul RS size
            2,  // int RS size
            1,  // loadBufSize - only 1 slot to force stall
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
            "L.D F6, 0(R2)",   // Issue cycle 1
            "L.D F2, 8(R2)",   // Stalled - no load buffer space until buffer frees (in-order: waits)
            "MUL.D F0, F2, F4" // Issues after L.D F2 (in-order issue)
        );
        
        sim.loadProgramLines(lines);
        
        System.out.println("Testing step() return values with in-order issue...\n");
        
        boolean testPassed = true;
        
        // Run simulation and track step() return values
        for (int i = 0; i < 30; i++) {
            int cycleNum = i + 1;
            boolean stepResult = sim.step();
            
            // Check if all instructions have completed
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            boolean allDone = true;
            boolean hasInFlight = false;
            for (SimulatorState.InstructionStatus s : statuses) {
                if (s.writeBackCycle <= 0) {
                    allDone = false;
                    if (s.issueCycle > 0) {
                        hasInFlight = true;
                    }
                }
            }
            
            System.out.println("Cycle " + cycleNum + ": step() returned " + stepResult + 
                             " (allDone=" + allDone + ", hasInFlight=" + hasInFlight + ")");
            
            // Key test: step() should return true when there are instructions in-flight
            // or when there are still instructions to issue (stalled or not)
            // Even if no new instruction is issued
            if (!allDone && !stepResult) {
                System.out.println("  ERROR: step() returned false when simulation should continue!");
                System.out.println("  There are still instructions in-flight or waiting.");
                testPassed = false;
            }
            
            if (allDone) {
                System.out.println("\n>>> All instructions complete <<<");
                // After all instructions complete, the next step() can return false
                break;
            }
        }
        
        // Final verification
        System.out.println("\n=== VERIFICATION ===");
        List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
        
        // Check that all instructions eventually completed
        boolean allCompleted = true;
        for (int j = 0; j < statuses.size(); j++) {
            SimulatorState.InstructionStatus s = statuses.get(j);
            System.out.println("  [" + j + "] Issue=" + s.issueCycle + 
                               " ExecStart=" + s.execStartCycle + 
                               " ExecEnd=" + s.execEndCycle +
                               " WriteBack=" + s.writeBackCycle);
            if (s.writeBackCycle <= 0) {
                allCompleted = false;
            }
        }
        
        if (allCompleted) {
            System.out.println("\nPASS: All instructions completed successfully");
        } else {
            System.out.println("\nFAIL: Some instructions did not complete");
            testPassed = false;
        }
        
        // Verify in-order issue: issue cycles should be strictly increasing
        int ldF6Issue = statuses.get(0).issueCycle;
        int ldF2Issue = statuses.get(1).issueCycle;
        int mulIssue = statuses.get(2).issueCycle;
        
        if (ldF6Issue < ldF2Issue && ldF2Issue < mulIssue) {
            System.out.println("PASS: Instructions issued in strict program order");
        } else {
            System.out.println("FAIL: Instructions did not issue in strict program order");
            testPassed = false;
        }
        
        System.out.println("\n=== TEST " + (testPassed ? "PASSED" : "FAILED") + " ===");
        
        // Exit with appropriate code for CI
        System.exit(testPassed ? 0 : 1);
    }
}
