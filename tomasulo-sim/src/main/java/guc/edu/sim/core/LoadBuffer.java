package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Load buffer for handling load instructions. 
 */
public class LoadBuffer {
    private final List<LoadEntry> buffer = new ArrayList<>();
    private final int maxSize;
    private final RegisterFile regFile;
    @SuppressWarnings("unused")
    private final Memory memory;
    @SuppressWarnings("unused")
    private final Cache cache;
    private int nextId = 1;
    private String lastAllocatedTag;

    public LoadBuffer(int maxSize, RegisterFile regFile, Memory memory, Cache cache) {
        this.maxSize = maxSize;
        this.regFile = regFile;
        this.memory = memory;
        this.cache = cache;
        System.out.println("[LoadBuffer] Initialized with size=" + maxSize);
    }

    public boolean hasFree() {
        return buffer.size() < maxSize;
    }

    public void accept(Instruction instr) {
        String tag = "LOAD" + nextId++;
        LoadEntry entry = new LoadEntry(tag, instr);
        
        // For load: address = offset + base register
        if (instr.getBase() != null) {
            String baseProducer = regFile.getProducer(instr.getBase());
            if (baseProducer == null) {
                entry.baseValue = regFile.getValue(instr.getBase());
                entry.baseReady = true;
            } else {
                entry.baseProducer = baseProducer;
            }
        }

        // Mark destination register as busy
        if (instr.getDest() != null) {
            regFile.setProducer(instr.getDest(), tag);
        }

        buffer.add(entry);
        lastAllocatedTag = tag;
        System.out.println("[LoadBuffer] Allocated " + tag + " for " + instr.getOpcode());
    }

    public List<LoadEntry> getBuffer() {
        return new ArrayList<>(buffer);
    }

    public void removeEntry(LoadEntry entry) {
        buffer.remove(entry);
    }

    public void broadcastResult(String tag, double result) {
        for (LoadEntry entry : buffer) {
            if (tag.equals(entry.baseProducer)) {
                entry.baseValue = result;
                entry.baseReady = true;
                entry.baseProducer = null;
            }
        }
    }

    public static class LoadEntry {
        public final String tag;
        public final Instruction instruction;
        public double baseValue;
        public boolean baseReady = false;
        public String baseProducer;
        public boolean executing = false;
        public int remainingCycles = 0;
        public double result;

        public LoadEntry(String tag, Instruction instruction) {
            this.tag = tag;
            this.instruction = instruction;
        }

        public boolean isReady() {
            return baseReady && !executing;
        }

        public int computeAddress() {
            int offset = instruction.getOffset() != null ? instruction.getOffset() : 0;
            return (int) baseValue + offset;
        }

        @Override
        public String toString() {
            return tag + " " + instruction.getOpcode() + " baseReady=" + baseReady + 
                   " executing=" + executing + " remaining=" + remainingCycles;
        }
    }

    public String getLastAllocatedTag() {
        return lastAllocatedTag;
    }
}