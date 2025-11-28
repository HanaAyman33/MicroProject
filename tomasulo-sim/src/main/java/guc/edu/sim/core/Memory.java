package guc. edu.sim.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Byte-addressable memory system. 
 */
public class Memory {
    private final Map<Integer, Byte> memory = new HashMap<>();

    public Memory() {
        // Initialize some memory
    }

    public void storeByte(int address, byte value) {
        memory.put(address, value);
        System.out.println("[Memory] Stored byte at address " + address + ": " + value);
    }

    public byte loadByte(int address) {
        return memory.getOrDefault(address, (byte) 0);
    }

    public void storeWord(int address, int value) {
        memory.put(address, (byte) (value & 0xFF));
        memory.put(address + 1, (byte) ((value >> 8) & 0xFF));
        memory.put(address + 2, (byte) ((value >> 16) & 0xFF));
        memory.put(address + 3, (byte) ((value >> 24) & 0xFF));
        System.out.println("[Memory] Stored word at address " + address + ": " + value);
    }

    public int loadWord(int address) {
        int b0 = memory.getOrDefault(address, (byte) 0) & 0xFF;
        int b1 = memory.getOrDefault(address + 1, (byte) 0) & 0xFF;
        int b2 = memory.getOrDefault(address + 2, (byte) 0) & 0xFF;
        int b3 = memory. getOrDefault(address + 3, (byte) 0) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    public void storeDouble(int address, double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 0; i < 8; i++) {
            memory. put(address + i, (byte) ((bits >> (8 * i)) & 0xFF));
        }
        System.out.println("[Memory] Stored double at address " + address + ": " + value);
    }

    public double loadDouble(int address) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            long b = memory.getOrDefault(address + i, (byte) 0) & 0xFF;
            bits |= (b << (8 * i));
        }
        return Double.longBitsToDouble(bits);
    }

    public void loadInitialData(Map<Integer, Double> initialData) {
        initialData.forEach(this::storeDouble);
    }

    public byte[] loadBlock(int blockStartAddress, int blockSize) {
        byte[] block = new byte[blockSize];
        for (int i = 0; i < blockSize; i++) {
            block[i] = loadByte(blockStartAddress + i);
        }
        return block;
    }
}