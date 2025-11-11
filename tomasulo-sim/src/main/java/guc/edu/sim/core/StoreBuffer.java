package guc.edu.sim.core;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Implements a store buffer for the Tomasulo simulator.
 * Handles store instructions, address calculation, and memory write operations.
 */
public class StoreBuffer implements MemoryUnitInterface {
    private final SimulationClock clock;
    private final RegisterFile registerFile;
    private final int storeLatency;
    private final int bufferSize;
    
    private static class StoreEntry {
        final Instruction instruction;
        final int address;
        final int value;
        int remainingCycles;
        
        StoreEntry(Instruction instruction, int address, int value, int latency) {
            this.instruction = instruction;
            this.address = address;
            this.value = value;
            this.remainingCycles = latency;
        }
    }
    
    private final Queue<StoreEntry> buffer;
    private final MemoryUnitInterface memoryUnit;
    
    public StoreBuffer(SimulationClock clock, RegisterFile registerFile, MemoryUnitInterface memoryUnit, 
                      int storeLatency, int bufferSize) {
        this.clock = clock;
        this.registerFile = registerFile;
        this.memoryUnit = memoryUnit;
        this.storeLatency = storeLatency;
        this.bufferSize = bufferSize;
        this.buffer = new ArrayDeque<>(bufferSize);
    }
    
    @Override
    public boolean hasFreeFor(Instruction instr) {
        return buffer.size() < bufferSize;
    }
    
    @Override
    public void accept(Instruction instr, RegisterStatusTable regStatus) {
        if (!hasFreeFor(instr)) {
            throw new IllegalStateException("Store buffer is full");
        }
        
        // Calculate memory address: offset + base register value
        int baseValue = registerFile.getRegisterValue(instr.getBase());
        int address = baseValue + (instr.getOffset() != null ? instr.getOffset() : 0);
        
        // Get value to store from register
        int value = registerFile.getRegisterValue(instr.getSrc1());
        
        buffer.add(new StoreEntry(instr, address, value, storeLatency));
    }
    
    public void cycle() {
        if (buffer.isEmpty()) return;
        
        StoreEntry entry = buffer.peek();
        entry.remainingCycles--;
        
        if (entry.remainingCycles <= 0) {
            // Commit the store to memory
            commitStore(entry);
            buffer.remove();
        }
    }
    
    private void commitStore(StoreEntry entry) {
        // In a real implementation, this would write to the memory unit
        // For now, we'll just print the store operation
        System.out.printf("Cycle %d: Store to address %d, value %d\n", 
                         clock.getCurrentCycle(), entry.address, entry.value);
        
        // In a full implementation, we would call memoryUnit.write(entry.address, entry.value);
    }
    
    public boolean hasAddressDependency(int address, int size) {
        // Check if any store in the buffer conflicts with the given address range
        for (StoreEntry entry : buffer) {
            // Simple range check - in a real implementation, consider the size of the access
            if (entry.address == address) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFull() {
        return buffer.size() >= bufferSize;
    }
    
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
}
