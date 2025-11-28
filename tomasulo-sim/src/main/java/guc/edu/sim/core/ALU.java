package guc.edu.sim.core;

/**
 * Arithmetic Logic Unit for computing instruction results.
 */
public class ALU {
    
    public static double compute(String opcode, double vj, double vk) {
        String op = opcode.toUpperCase();
        
        // Remove . D suffix if present
        if (op.endsWith(".D")) {
            op = op.substring(0, op.length() - 2);
        }
        
        switch (op) {
            case "ADD":
            case "DADD":
            case "DADDI":
            case "ADDI":
                System.out.println("[ALU] " + vj + " + " + vk + " = " + (vj + vk));
                return vj + vk;
                
            case "SUB":
            case "DSUB":
            case "SUBI":
            case "DSUBI":
                System.out. println("[ALU] " + vj + " - " + vk + " = " + (vj - vk));
                return vj - vk;
                
            case "MUL":
            case "DMUL":
                System.out.println("[ALU] " + vj + " * " + vk + " = " + (vj * vk));
                return vj * vk;
                
            case "DIV":
            case "DDIV":
                if (vk == 0) {
                    System.out.println("[ALU] Division by zero!");
                    return Double.NaN;
                }
                System.out.println("[ALU] " + vj + " / " + vk + " = " + (vj / vk));
                return vj / vk;
                
            default:
                System.out.println("[ALU] Unknown operation: " + opcode);
                return 0.0;
        }
    }
}