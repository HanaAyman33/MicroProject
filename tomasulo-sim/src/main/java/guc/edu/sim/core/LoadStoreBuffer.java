package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Load/Store buffer implementation with address calculation and cache integration.
 */
public class LoadStoreBuffer implements MemoryUnitInterface {
    private final List<LoadStoreEntry> buffer = new ArrayList<>();
    private final int maxSize;
    private final RegisterFile regFile;
    private final Memory memory;
    private final Cache cache;
    private int nextId = 1;

    public LoadStoreBuffer(int maxSize, RegisterFile regFile, Memory memory, Cache cache) {
        this.maxSize = maxSize;
        this.regFile = regFile;
        this.memory = memory;
        this.cache = cache;
        System.out. println("[LoadStore] Initialized with size=" + maxSize);
    }

    @Override
    public boolean hasFreeFor(Instruction instr) {
        return buffer.size() < maxSize;
    }

    @Override
    public void accept(Instruction instr, RegisterStatusTable regStatus) {
        String tag = "LS" + nextId++;
        LoadStoreEntry entry = new LoadStoreEntry(tag, instr);
        
        // For load/store: address = offset + base register
        if (instr.getBase() != null) {
            String baseProducer = regFile.getProducer(instr.getBase());
            if (baseProducer == null) {
                entry.baseValue = regFile.getValue(instr.getBase());
                entry.baseReady = true;
            } else {
                entry.baseProducer = baseProducer;
            }
        }

        // For stores, also need the value to store
        if (instr.getType() == InstructionType.STORE && instr.getSrc1() != null) {
            String srcProducer = regFile. getProducer(instr. getSrc1());
            if (srcProducer == null) {
                entry.storeValue = regFile.getValue(instr.getSrc1());
                entry.storeReady = true;
            } else {
                entry.storeProducer = srcProducer;
            }
        }

        // Mark destination for loads
        if (instr.getType() == InstructionType. LOAD && instr.getDest() != null) {
            regFile.setProducer(instr.getDest(), tag);
        }

        buffer.add(entry);
        System.out. println("[LoadStore] Allocated " + tag + " for " + instr.getOpcode());
    }

    public List<LoadStoreEntry> getBuffer() {
        return new ArrayList<>(buffer);
    }

    public void removeEntry(LoadStoreEntry entry) {
        buffer.remove(entry);
    }

    public void broadcastResult(String tag, double result) {
        for (LoadStoreEntry entry : buffer) {
            if (tag. equals(entry.baseProducer)) {
                entry.baseValue = result;
                entry.baseReady = true;
                entry.baseProducer = null;
            }
            if (tag.equals(entry.storeProducer)) {
                entry.storeValue = result;
                entry.storeReady = true;
                entry.storeProducer = null;
            }
        }
    }

    public static class LoadStoreEntry {
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
        public double result;

        public LoadStoreEntry(String tag, Instruction instruction) {
            this.tag = tag;
            this.instruction = instruction;
        }

        public boolean isReady() {
            if (instruction.getType() == InstructionType.LOAD) {
                return baseReady && ! executing;
            } else {
                return baseReady && storeReady && !executing;
            }
        }

        public int computeAddress() {
            int offset = instruction.getOffset() != null ? instruction.getOffset() : 0;
            return (int) baseValue + offset;
        }
    }
}