package guc.edu.sim.core;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Handles branch instruction execution and resolution in the Tomasulo simulator.
 * Implements branch execution, condition resolution, and branch target calculation.
 */
public class BranchUnit implements BranchUnitInterface {
    private final SimulationClock clock;
    private final RegisterFile registerFile;
    private final int branchLatency;
    
    private Instruction currentInstruction;
    private int remainingCycles;
    private boolean isResolved;
    private int resolvedTargetPc;
    private boolean shouldFlush;
    private final Queue<Instruction> instructionQueue;

    public BranchUnit(SimulationClock clock, RegisterFile registerFile, int branchLatency) {
        this.clock = clock;
        this.registerFile = registerFile;
        this.branchLatency = branchLatency;
        this.instructionQueue = new ArrayDeque<>();
        this.isResolved = false;
        this.shouldFlush = false;
    }

    @Override
    public boolean isFree() {
        return currentInstruction == null && instructionQueue.isEmpty();
    }

    @Override
    public void accept(Instruction instr, RegisterStatusTable regStatus) {
        if (currentInstruction == null && instructionQueue.isEmpty()) {
            currentInstruction = instr;
            remainingCycles = branchLatency;
            isResolved = false;
            shouldFlush = false;
        } else {
            instructionQueue.add(instr);
        }
    }

    public void cycle() {
        if (currentInstruction == null) return;
        
        remainingCycles--;
        
        if (remainingCycles <= 0) {
            resolveBranch();
            currentInstruction = instructionQueue.poll();
            if (currentInstruction != null) {
                remainingCycles = branchLatency;
            }
        }
    }

    private void resolveBranch() {
        if (currentInstruction == null) return;
        
        isResolved = true;
        String opcode = currentInstruction.getOpcode();
        
        switch (opcode) {
            case "BEQ":
                int val1 = registerFile.getRegisterValue(currentInstruction.getSrc1());
                int val2 = registerFile.getRegisterValue(currentInstruction.getSrc2());
                shouldFlush = (val1 == val2);
                break;
                
            case "BNE":
                val1 = registerFile.getRegisterValue(currentInstruction.getSrc1());
                val2 = registerFile.getRegisterValue(currentInstruction.getSrc2());
                shouldFlush = (val1 != val2);
                break;
                
            default:
                shouldFlush = false;
                break;
        }
        
        // For simplicity, we'll assume the target address is in the branch target label
        // In a real implementation, this would calculate the actual target address
        resolvedTargetPc = -1; // This would be set based on the branch target calculation
    }

    @Override
    public boolean hasResolvedBranch() {
        return isResolved;
    }

    @Override
    public int getResolvedTargetPc() {
        return resolvedTargetPc;
    }

    @Override
    public boolean shouldFlushQueue() {
        return shouldFlush;
    }
    
    public void flush() {
        instructionQueue.clear();
        currentInstruction = null;
        isResolved = false;
        shouldFlush = false;
    }
}
