package guc.edu.sim.core;

/**
 * Branch unit for handling BEQ/BNE instructions without prediction.
 */
public class BranchUnit implements BranchUnitInterface {
    private final RegisterFile regFile;
    private final Program program;
    private boolean busy = false;
    private Instruction currentBranch;
    private double src1Val, src2Val;
    private boolean src1Ready, src2Ready;
    private String src1Producer, src2Producer;
    private int targetPc = -1;
    private boolean resolved = false;
    private boolean shouldBranch = false;
    private int latency = 1;
    private int remainingCycles = 0;
    private int readyCycle = -1;  // cycle when all operands became ready (-1 means ready at issue or not yet)

    public BranchUnit(RegisterFile regFile, Program program) {
        this.regFile = regFile;
        this.program = program;
    }

    @Override
    public boolean isFree() {
        return !busy;
    }

    @Override
    public void accept(Instruction instr, RegisterStatusTable regStatus) {
        busy = true;
        currentBranch = instr;
        resolved = false;
        remainingCycles = latency;
        readyCycle = -1;  // Reset readyCycle for new instruction
        
        // Read operands
        String s1 = instr.getSrc1();
        String s2 = instr.getSrc2();
        
        if (s1 != null) {
            String prod = regFile.getProducer(s1);
            if (prod == null) {
                src1Val = regFile.getValue(s1);
                src1Ready = true;
            } else {
                src1Producer = prod;
                src1Ready = false;
            }
        } else {
            src1Ready = true;
        }
        
        if (s2 != null) {
            String prod = regFile.getProducer(s2);
            if (prod == null) {
                src2Val = regFile.getValue(s2);
                src2Ready = true;
            } else {
                src2Producer = prod;
                src2Ready = false;
            }
        } else {
            src2Ready = true;
        }
        
        System.out.println("[Branch] Accepted " + instr.getOpcode() + " waiting for operands");
    }

    public void tryResolve() {
        tryResolve(-1);
    }
    
    public void tryResolve(int currentCycle) {
        if (!busy || resolved || !src1Ready || !src2Ready) return;
        
        // If operands were received from CDB, wait until the next cycle to start execution
        if (readyCycle >= 0 && currentCycle >= 0 && currentCycle <= readyCycle) {
            return;
        }
        
        if (remainingCycles > 0) {
            remainingCycles--;
            return;
        }
        
        boolean condition = false;
        String opcode = currentBranch.getOpcode(). toUpperCase();
        
        if (opcode.equals("BEQ")) {
            condition = (src1Val == src2Val);
        } else if (opcode.equals("BNE")) {
            condition = (src1Val != src2Val);
        }
        
        shouldBranch = condition;
        if (condition) {
            String label = currentBranch.getBranchTargetLabel();
            targetPc = program.getLabelIndex(label);
        }
        
        resolved = true;
        System.out.println("[Branch] Resolved: " + opcode + " condition=" + condition + " target=" + targetPc);
    }

    public void broadcastResult(String tag, double result) {
        broadcastResult(tag, result, -1);
    }
    
    public void broadcastResult(String tag, double result, int currentCycle) {
        if (tag.equals(src1Producer)) {
            src1Val = result;
            src1Ready = true;
            src1Producer = null;
        }
        if (tag.equals(src2Producer)) {
            src2Val = result;
            src2Ready = true;
            src2Producer = null;
        }
        // Record when operands became ready from CDB
        if (currentCycle >= 0 && src1Ready && src2Ready && readyCycle < 0) {
            readyCycle = currentCycle;
        }
    }

    public void clear() {
        busy = false;
        resolved = false;
        currentBranch = null;
        shouldBranch = false;
        targetPc = -1;
        remainingCycles = 0;
        readyCycle = -1;
        src1Ready = false;
        src2Ready = false;
        src1Producer = null;
        src2Producer = null;
    }

    @Override
    public boolean hasResolvedBranch() {
        return resolved;
    }

    @Override
    public int getResolvedTargetPc() {
        return shouldBranch ? targetPc : -1;
    }

    @Override
    public boolean shouldFlushQueue() {
        return shouldBranch;
    }

    public void setLatency(int latency) {
        this.latency = Math.max(1, latency);
    }
}