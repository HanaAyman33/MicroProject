package guc.edu.sim.core;

import java.util.*;

/**
 * Test for in-order issue per Tomasulo's algorithm.
 * This test verifies that instructions issue in strict program order:
 * - If an instruction cannot be issued due to structural hazard, the issue stage stalls
 * - The next instruction does NOT issue until the stalled instruction can be issued
 */
public class OutOfOrderIssueTest {
    
    public static void main(String[] args) {
        System.out.println("=== In-Order Issue Test ===\n");
        
        // Test case: Load buffer has only 1 slot
        // L.D F6, 0(R2)   - will use the only load buffer slot (cycle 1)
        // L.D F2, 8(R2)   - will stall until load buffer frees (strict in-order - waits)
        // MUL.D F0, F2, F4 - should issue AFTER L.D F2 (in-order issue)
        // SUB.D F8, F2, F6 - should issue AFTER MUL.D (in-order issue)
        
        SimulatorState sim = new SimulatorState();
        
        // Set up configuration with only 1 load buffer slot to force the stall
        // Args: fpAdd, fpMul, intAlu, loadBufSize, storeBufSize, 
        //       cacheSz, blockSz, hitLat, missPen,
        //       fpAddLat, fpMulLat, fpDivLat, intLat, loadLat, storeLat, branchLat
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
            "L.D F6, 0(R2)",
            "L.D F2, 8(R2)", 
            "MUL.D F0, F2, F4",
            "SUB.D F8, F2, F6"
        );
        
        sim.loadProgramLines(lines);
        
        // Track what cycle each instruction was issued
        System.out.println("Running simulation for 30 cycles...\n");
        
        for (int i = 0; i < 30; i++) {
            System.out.println("\n==================== CYCLE " + (i+1) + " ====================");
            sim.step();  // Don't check return value, just run for the specified cycles
            
            // Print current issue status
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            System.out.println("\nInstruction Status Summary:");
            for (int j = 0; j < statuses.size(); j++) {
                SimulatorState.InstructionStatus s = statuses.get(j);
                System.out.println("  [" + j + "] Issue=" + s.issueCycle + 
                                   " ExecStart=" + s.execStartCycle + 
                                   " ExecEnd=" + s.execEndCycle +
                                   " WriteBack=" + s.writeBackCycle +
                                   " Tag=" + s.tag);
            }
            
            // Check if all instructions have completed
            boolean allDone = true;
            for (SimulatorState.InstructionStatus s : statuses) {
                if (s.writeBackCycle <= 0) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                System.out.println("\n>>> All instructions complete <<<");
                break;
            }
        }
        
        // Verify expectations for IN-ORDER issue
        System.out.println("\n\n=== VERIFICATION (In-Order Issue) ===");
        List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
        
        boolean testPassed = true;
        
        int ldF6Issue = statuses.get(0).issueCycle;
        int ldF2Issue = statuses.get(1).issueCycle;
        int mulIssue = statuses.get(2).issueCycle;
        int subIssue = statuses.get(3).issueCycle;
        
        // Check that L.D F6 was issued in cycle 1
        if (ldF6Issue != 1) {
            System.out.println("FAIL: L.D F6 should be issued in cycle 1, was: " + ldF6Issue);
            testPassed = false;
        } else {
            System.out.println("PASS: L.D F6 issued in cycle 1");
        }
        
        // For in-order issue: L.D F2 should issue BEFORE MUL.D and SUB.D
        // (MUL.D and SUB.D must wait for L.D F2 to issue first)
        if (ldF2Issue > 0) {
            System.out.println("PASS: L.D F2 eventually issued in cycle " + ldF2Issue);
            
            // MUL.D should issue AFTER L.D F2 (in-order issue)
            if (mulIssue > ldF2Issue) {
                System.out.println("PASS: MUL.D issued in cycle " + mulIssue + " (after L.D F2 in cycle " + ldF2Issue + ")");
            } else if (mulIssue == ldF2Issue + 1) {
                System.out.println("PASS: MUL.D issued in cycle " + mulIssue + " (immediately after L.D F2)");
            } else {
                System.out.println("FAIL: MUL.D issued in cycle " + mulIssue + ", expected after L.D F2 (cycle " + ldF2Issue + ")");
                testPassed = false;
            }
            
            // SUB.D should issue AFTER MUL.D (in-order issue)
            if (subIssue > mulIssue) {
                System.out.println("PASS: SUB.D issued in cycle " + subIssue + " (after MUL.D in cycle " + mulIssue + ")");
            } else if (subIssue == mulIssue + 1) {
                System.out.println("PASS: SUB.D issued in cycle " + subIssue + " (immediately after MUL.D)");
            } else {
                System.out.println("FAIL: SUB.D issued in cycle " + subIssue + ", expected after MUL.D (cycle " + mulIssue + ")");
                testPassed = false;
            }
        } else {
            System.out.println("FAIL: L.D F2 was never issued (expected after L.D F6 write-back frees buffer)");
            testPassed = false;
        }
        
        // Key test: Instructions should issue in strict order
        if (ldF6Issue < ldF2Issue && ldF2Issue < mulIssue && mulIssue < subIssue) {
            System.out.println("PASS: Instructions issued in strict program order (in-order issue)");
        } else {
            System.out.println("FAIL: Instructions did NOT issue in strict program order");
            System.out.println("  L.D F6: cycle " + ldF6Issue);
            System.out.println("  L.D F2: cycle " + ldF2Issue);
            System.out.println("  MUL.D:  cycle " + mulIssue);
            System.out.println("  SUB.D:  cycle " + subIssue);
            testPassed = false;
        }
        
        System.out.println("\n=== TEST " + (testPassed ? "PASSED" : "FAILED") + " ===");
        
        // Exit with appropriate code for CI
        System.exit(testPassed ? 0 : 1);
    }
}
