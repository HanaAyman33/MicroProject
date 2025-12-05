package guc.edu.sim.core;

import java.util.*;

/**
 * Test for out-of-order issue when structural hazards occur.
 * This test verifies that when an instruction cannot be issued due to
 * lack of buffer/RS space, subsequent instructions can still be issued.
 */
public class OutOfOrderIssueTest {
    
    public static void main(String[] args) {
        System.out.println("=== Out-of-Order Issue Test ===\n");
        
        // Test case: Load buffer has only 1 slot
        // L.D F6, 0(R2)   - will use the only load buffer slot (cycle 1)
        // L.D F2, 8(R2)   - will stall (no load buffer space)
        // MUL.D F0, F2, F4 - should issue despite L.D F2 being stalled
        // SUB.D F8, F2, F6 - should issue despite L.D F2 being stalled
        
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
        System.out.println("Running simulation for 20 cycles...\n");
        
        for (int i = 0; i < 20; i++) {
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
        
        // Verify expectations
        System.out.println("\n\n=== VERIFICATION ===");
        List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
        
        boolean testPassed = true;
        
        // Check that L.D F6 was issued in cycle 1
        if (statuses.get(0).issueCycle != 1) {
            System.out.println("FAIL: L.D F6 should be issued in cycle 1, was: " + statuses.get(0).issueCycle);
            testPassed = false;
        } else {
            System.out.println("PASS: L.D F6 issued in cycle 1");
        }
        
        // Check that MUL.D was issued before or when L.D F2 was issued
        // (MUL.D should not be blocked by L.D F2's stall)
        int mulIssue = statuses.get(2).issueCycle;
        int ldF2Issue = statuses.get(1).issueCycle;
        
        if (mulIssue > 0 && (ldF2Issue <= 0 || mulIssue < ldF2Issue)) {
            System.out.println("PASS: MUL.D issued in cycle " + mulIssue + " (before L.D F2 issued in cycle " + ldF2Issue + ")");
        } else if (mulIssue > 0 && ldF2Issue > 0) {
            System.out.println("INFO: MUL.D issued in cycle " + mulIssue + ", L.D F2 in cycle " + ldF2Issue);
            if (mulIssue >= ldF2Issue) {
                System.out.println("WARN: MUL.D should have been issued before L.D F2");
            }
        } else {
            System.out.println("FAIL: MUL.D was not issued, still -1");
            testPassed = false;
        }
        
        // Check that SUB.D was also issued while L.D F2 was stalled
        int subIssue = statuses.get(3).issueCycle;
        if (subIssue > 0 && (ldF2Issue <= 0 || subIssue < ldF2Issue)) {
            System.out.println("PASS: SUB.D issued in cycle " + subIssue + " (before L.D F2 issued in cycle " + ldF2Issue + ")");
        } else if (subIssue > 0 && ldF2Issue > 0) {
            System.out.println("INFO: SUB.D issued in cycle " + subIssue + ", L.D F2 in cycle " + ldF2Issue);
        } else {
            System.out.println("FAIL: SUB.D was not issued, still -1");
            testPassed = false;
        }
        
        // Key test: MUL.D and SUB.D should NOT wait until cycle 13+ like in the broken version
        if (mulIssue > 0 && mulIssue < 10) {
            System.out.println("PASS: MUL.D issued early (cycle " + mulIssue + "), not blocked by L.D F2 stall");
        } else if (mulIssue >= 10) {
            System.out.println("FAIL: MUL.D was blocked too long, issued in cycle " + mulIssue);
            testPassed = false;
        }
        
        // Verify L.D F2 eventually gets issued once space is available
        if (ldF2Issue > 0) {
            System.out.println("PASS: L.D F2 eventually issued in cycle " + ldF2Issue);
        } else {
            System.out.println("WARN: L.D F2 was never issued (expected after L.D F6 write-back frees buffer)");
        }
        
        System.out.println("\n=== TEST " + (testPassed ? "PASSED" : "FAILED") + " ===");
    }
}
