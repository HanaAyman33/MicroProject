package guc.edu.sim.core;


public class Instruction {

    private final String label;
    private final String opcode;
    private final InstructionType type;

    private final String dest;
    private final String src1;
    private final String src2;
    private final Integer offset;
    private final String base;
    private final String branchTargetLabel;

    private int issueCycle = -1;

    public Instruction(String label,
                       String opcode,
                       InstructionType type,
                       String dest,
                       String src1,
                       String src2,
                       Integer offset,
                       String base,
                       String branchTargetLabel) {
        this.label = label;
        this.opcode = opcode.toUpperCase();
        this.type = type;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
        this.offset = offset;
        this.base = base;
        this.branchTargetLabel = branchTargetLabel;
    }

    public String getLabel() { return label; }
    public String getOpcode() { return opcode; }
    public InstructionType getType() { return type; }
    public String getDest() { return dest; }
    public String getSrc1() { return src1; }
    public String getSrc2() { return src2; }
    public Integer getOffset() { return offset; }
    public String getBase() { return base; }
    public String getBranchTargetLabel() { return branchTargetLabel; }

    public int getIssueCycle() { return issueCycle; }
    public void setIssueCycle(int cycle) { this.issueCycle = cycle; }
}