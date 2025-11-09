package guc.edu.sim.core;
import java.util.*;

public class Program {

    private final List<Instruction> instructions;
    private final Map<String, Integer> labelToIndex;

    public Program(List<Instruction> instructions, Map<String, Integer> labelToIndex) {
        this.instructions = instructions;
        this.labelToIndex = labelToIndex;
    }

    public Instruction get(int pc) {
        return instructions.get(pc);
    }

    public int size() {
        return instructions.size();
    }

    public int getLabelIndex(String label) {
        return labelToIndex.get(label);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }
}