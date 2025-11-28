package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Store buffer for handling store instructions.
 */
public class StoreBuffer {
    private final List<StoreEntry> buffer = new ArrayList<>();
    private final int maxSize;
    private final RegisterFile regFile;
    private final Memory memory;
    private final Cache cache;
    private int nextId = 1;

    public StoreBuffer(int maxSize, RegisterFile regFile, Memory memory, Cache cache) {
        this.maxSize = maxSize;
        this.regFile = regFile;
        this.memory = memory;
        this.cache = cache;
        System.out. println("[StoreBuffer] Initialized with size=" + maxSize);
    }

    public boolean hasFree() {
        return buffer. size() < maxSize;
    }

    public void accept(Instruction instr) {
        String tag = "STORE" + nextId++;
        StoreEntry entry = new StoreEntry(tag, instr);
        
        // For store: address = offset + base register
        if (instr. getBase() != null) {
            String baseProducer = regFile.getProducer(instr.getBase());
            if (baseProducer == null) {
                entry.baseValue = regFile.getValue(instr. getBase());
                entry.baseReady = true;
            } else {
                entry.baseProducer = baseProducer;
            }
        }

        // Get the value to store (from src1)
        if (instr.getSrc1() != null) {
            String srcProducer = regFile.getProducer(instr.getSrc1());
            if (srcProducer == null) {
                entry.storeValue = regFile.getValue(instr.getSrc1());
                entry.storeReady = true;
            } else {
                entry.storeProducer = srcProducer;
            }
        }

        buffer.add(entry);
        System.out.println("[StoreBuffer] Allocated " + tag + " for " + instr.getOpcode());
    }

    public List<StoreEntry> getBuffer() {
        return new ArrayList<>(buffer);
    }

    public void removeEntry(StoreEntry entry) {
        buffer.remove(entry);
    }

    public void broadcastResult(String tag, double result) {
        for (StoreEntry entry : buffer) {
            if (tag.equals(entry.baseProducer)) {
                entry.baseValue = result;
                entry.baseReady = true;
                entry. baseProducer = null;
            }
            if (tag.equals(entry. storeProducer)) {
                entry.storeValue = result;
                entry.storeReady = true;
                entry. storeProducer = null;
            }
        }
    }

    public static class StoreEntry {
        public final String tag;
        public final Instruction instruction;
        public double baseValue;
        public boolean baseReady = false;
        public String baseProducer;
        public double storeValue;
        public boolean storeReady = false;
        public String storeProducer;
        public boolean executing = false;
        public int remainingCycles = 0;

        public StoreEntry(String tag, Instruction instruction) {
            this.tag = tag;
            this.instruction = instruction;
        }

        public boolean isReady() {
            return baseReady && storeReady && !executing;
        }

        public int computeAddress() {
            int offset = instruction.getOffset() != null ? instruction.getOffset() : 0;
            return (int) baseValue + offset;
        }

        @Override
        public String toString() {
            return tag + " " + instruction. getOpcode() + " baseReady=" + baseReady + 
                   " storeReady=" + storeReady + " executing=" + executing + 
                   " remaining=" + remainingCycles;
        }
    }
}