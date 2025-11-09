package guc.edu.sim.core;


import java.util.*;

public class ProgramLoader {

    public Program loadFromLines(List<String> lines) {
        return parseLines(lines);
    }

    private Program parseLines(List<String> lines) {
        Map<String, Integer> labelToIndex = new HashMap<>();
        List<String> clean = new ArrayList<>();

        // pass 1: extract labels
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                labelToIndex.put(label, clean.size());
            } else if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String label = parts[0].trim();
                String rest = parts[1].trim();
                labelToIndex.put(label, clean.size());
                if (!rest.isEmpty()) clean.add(rest);
            } else {
                clean.add(line);
            }
        }

        // pass 2: build instructions
        List<Instruction> instructions = new ArrayList<>();
        for (String line : clean) {
            instructions.add(parseInstruction(line));
        }

        return new Program(instructions, labelToIndex);
    }

    private Instruction parseInstruction(String line) {
        String[] t = line.replace(",", " ")
                         .replace("(", " ")
                         .replace(")", " ")
                         .trim()
                         .split("\\s+");

        String opcode = t[0].toUpperCase();
        InstructionType type = classify(opcode);

        String dest = null, src1 = null, src2 = null, base = null, labelTarget = null;
        Integer offset = null;

        switch (type) {
            case LOAD:
                // L.D F6, 8(R2)
                dest = t[1];
                offset = Integer.parseInt(t[2]);
                base = t[3];
                break;

            case STORE:
                // S.D F6, 8(R2)
                src1 = t[1];
                offset = Integer.parseInt(t[2]);
                base = t[3];
                break;

            case ALU_FP:
            case ALU_INT:
                // MUL.D F0, F2, F4 or DADDI R1, R1, 24
                dest = t[1];
                src1 = t[2];
                if (t.length > 3) src2 = t[3];
                break;

            case BRANCH:
                // BEQ R1, R2, LOOP
                src1 = t[1];
                src2 = t[2];
                labelTarget = t[3];
                break;

            default:
                break;
        }

        return new Instruction(null, opcode, type, dest, src1, src2, offset, base, labelTarget);
    }

    private InstructionType classify(String op) {
        String opcode = op.toUpperCase();
        if (opcode.equals("L.D") || opcode.equals("LW") || opcode.equals("L.S"))
            return InstructionType.LOAD;
        if (opcode.equals("S.D") || opcode.equals("SW"))
            return InstructionType.STORE;
        if (opcode.equals("BEQ") || opcode.equals("BNE"))
            return InstructionType.BRANCH;
        if (opcode.contains("ADD") || opcode.contains("SUB") ||
            opcode.contains("MUL") || opcode.contains("DIV")) {
            if (opcode.endsWith(".D")) return InstructionType.ALU_FP;
            return InstructionType.ALU_INT;
        }
        return InstructionType.UNKNOWN;
    }
}