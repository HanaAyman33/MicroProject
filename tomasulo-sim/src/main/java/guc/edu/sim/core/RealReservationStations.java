package guc.edu.sim.core;

import java. util. ArrayList;
import java.util.List;

/**
 * Real implementation of reservation stations for FP and integer ALU operations.
 */
public class RealReservationStations implements ReservationStations {
    private final List<ReservationStationEntry> stations = new ArrayList<>();
    private final int fpAddSize;
    private final int fpMulSize;
    private final int intSize;
    private final RegisterFile regFile;
    private String lastAllocatedTag;

    private int nextFpAddId = 1;
    private int nextFpMulId = 1;
    private int nextIntId = 1;

    public RealReservationStations(int fpAddSize, int fpMulSize, int intSize, RegisterFile regFile) {
        this.fpAddSize = fpAddSize;
        this.fpMulSize = fpMulSize;
        this.intSize = intSize;
        this.regFile = regFile;
        System.out.println("[RS] Initialized: FP_ADD=" + fpAddSize + ", FP_MUL=" + fpMulSize + ", INT=" + intSize);
    }

    @Override
    public boolean hasFreeFor(Instruction instr) {
        StationType type = getStationType(instr);
        int max = getMaxSize(type);
        long count = stations.stream(). filter(s -> s.getType() == type). count();
        return count < max;
    }

    @Override
    public void accept(Instruction instr, RegisterStatusTable regStatus) {
        StationType type = getStationType(instr);
        String tag;
        switch (type) {
            case FP_ADD:
                tag = "A" + nextFpAddId++;
                break;
            case FP_MUL:
                tag = "M" + nextFpMulId++;
                break;
            case INTEGER:
            default:
                tag = "I" + nextIntId++;
                break;
        }
        
        // Create entry with instruction reference
        ReservationStationEntry entry = new ReservationStationEntry(
            tag, type, instr. getOpcode(), instr.getDest(), instr
        );

        // Read operands from register file
        String src1 = instr.getSrc1();
        String src2 = instr. getSrc2();

        if (src1 != null) {
            String producer = regFile.getProducer(src1);
            if (producer == null) {
                Double value = regFile.getValue(src1);
                if (value != null) {
                    entry.setVj(value);
                }
            } else {
                entry.setQj(producer);
            }
        }

        if (src2 != null) {
            // Check if src2 is immediate value
            if (src2.matches("-?\\d+")) {
                entry.setVk(Double.parseDouble(src2));
            } else {
                String producer = regFile.getProducer(src2);
                if (producer == null) {
                    Double value = regFile.getValue(src2);
                    if (value != null) {
                        entry.setVk(value);
                    }
                } else {
                    entry.setQk(producer);
                }
            }
        }

        // Mark destination as busy
        if (instr.getDest() != null) {
            regFile.setProducer(instr.getDest(), tag);
        }

        stations.add(entry);
        lastAllocatedTag = tag;
        System.out.println("[RS] Allocated " + tag + " for " + instr. getOpcode() + " -> " + entry);
    }

    private StationType getStationType(Instruction instr) {
        String op = instr.getOpcode().toUpperCase();
        boolean isFloat = op.contains(".D") || op.contains(".S");

        if (op.contains("MUL") || op.contains("DIV")) {
            return isFloat ? StationType.FP_MUL : StationType.INTEGER;
        }
        if (op.contains("ADD") || op.contains("SUB")) {
            return isFloat ? StationType.FP_ADD : StationType.INTEGER;
        }
        return StationType.INTEGER;
    }

    private int getMaxSize(StationType type) {
        switch (type) {
            case FP_ADD: return fpAddSize;
            case FP_MUL: return fpMulSize;
            case INTEGER: return intSize;
            default: return 0;
        }
    }

    public List<ReservationStationEntry> getStations() {
        return new ArrayList<>(stations);
    }

    public void removeEntry(ReservationStationEntry entry) {
        stations.remove(entry);
    }

    public void broadcastResult(String tag, double result) {
        broadcastResult(tag, result, -1);
    }
    
    public void broadcastResult(String tag, double result, int currentCycle) {
        ReservationStationEntry selfToRemove = null;
        for (ReservationStationEntry entry : stations) {
            // When the producing instruction's own result is broadcast, we
            // free its reservation-station slot *after* write-back. This
            // models structural hazards correctly: new instructions cannot
            // reuse this station until the value has been written back.
            if (tag.equals(entry.getId())) {
                selfToRemove = entry;
                continue;
            }
            if (tag.equals(entry.getQj())) {
                if (currentCycle >= 0) {
                    entry.setVj(result, currentCycle);
                } else {
                    entry.setVj(result);
                }
            }
            if (tag.equals(entry.getQk())) {
                if (currentCycle >= 0) {
                    entry.setVk(result, currentCycle);
                } else {
                    entry.setVk(result);
                }
            }
        }
        if (selfToRemove != null) {
            stations.remove(selfToRemove);
        }
    }

    public String getLastAllocatedTag() {
        return lastAllocatedTag;
    }
}
 