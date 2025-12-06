package guc.edu.sim.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Direct-mapped cache implementation with proper miss handling.
 * The cache state is only updated AFTER the miss penalty completes.
 */
public class Cache {
    private final int cacheSize;      // Total cache size in bytes
    private final int blockSize;      // Block size in bytes
    private final int numBlocks;      // Number of cache lines
    private final int hitLatency;
    private final int missPenalty;
    
    private final CacheLine[] lines;
    private int hits = 0;
    private int misses = 0;
    
    // Track pending cache fills (block address -> cycle when fill completes)
    private final Map<Integer, Integer> pendingFills = new HashMap<>();

    public Cache(int cacheSize, int blockSize, int hitLatency, int missPenalty) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.numBlocks = cacheSize / blockSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
        this.lines = new CacheLine[numBlocks];
        
        for (int i = 0; i < numBlocks; i++) {
            lines[i] = new CacheLine(blockSize);
        }
        
        System.out.println("[Cache] Initialized: " + numBlocks + " lines, " + blockSize + " bytes/block");
    }

    /**
     * Access the cache. Returns the latency and whether it's a hit.
     * On a miss, the cache line is NOT updated immediately - it's marked as pending.
     */
 // In Cache.java, access() method - change the miss case:

    public CacheAccessResult access(int address, Memory memory) {
        int blockAddress = (address / blockSize) * blockSize;
        int index = (address / blockSize) % numBlocks;
        int tag = address / cacheSize;
        
        CacheLine line = lines[index];
        
        // Check if this line is currently being filled
        if (pendingFills.containsKey(blockAddress)) {
            // Still loading from a previous miss - treat as miss
            misses++;
            System.out.println("[Cache] MISS at address " + address + " (block " + blockAddress + " still loading from previous miss)");
            
            // Load block from memory (we need the data reference)
            byte[] block = memory.loadBlock(blockAddress, blockSize);
            return new CacheAccessResult(false, missPenalty, block, blockAddress);
        }
        
        if (line.isValid() && line.getTag() == tag) {
            // Cache hit
            hits++;
            System.out.println("[Cache] HIT at address " + address + " (index=" + index + ", tag=" + tag + ")");
            return new CacheAccessResult(true, hitLatency, line.getData(), blockAddress);
        } else {
            // Cache miss - DON'T update cache line yet
            misses++;
            System.out.println("[Cache] MISS at address " + address + " (index=" + index + ", tag=" + tag + ")");
            
            // Load block from memory
            byte[] block = memory.loadBlock(blockAddress, blockSize);
            
            // FIXED: Return missPenalty only, not hitLatency + missPenalty
            return new CacheAccessResult(false, missPenalty, block, blockAddress);
        }
    }
    
    /**
     * Complete a cache fill after the miss penalty has elapsed.
     * This should be called when a load instruction completes.
     */
    public void completeFill(int address, Memory memory) {
        int blockAddress = (address / blockSize) * blockSize;
        int index = (address / blockSize) % numBlocks;
        int tag = address / cacheSize;
        
        CacheLine line = lines[index];
        byte[] block = memory.loadBlock(blockAddress, blockSize);
        
        System.out.println("[Cache] completeFill called for address " + address);
        System.out.println("[Cache]   blockAddress=" + blockAddress + ", index=" + index + ", tag=" + tag);
        System.out.println("[Cache]   Block data from memory: " + java.util.Arrays.toString(block));
        
       
        line.setValid(true);
        line.setTag(tag);
        line.setData(block);
        
        // Remove from pending
        pendingFills.remove(blockAddress);
        
        System.out.println("[Cache] Completed fill for address " + address + " (index=" + index + ", tag=" + tag + ")");
        debugPrintCacheLine(index, line);
    }
    
    /**
     * Mark a block address as pending fill (started loading but not complete)
     */
    public void markPendingFill(int address, int completionCycle) {
        int blockAddress = (address / blockSize) * blockSize;
        pendingFills.put(blockAddress, completionCycle);
        System.out.println("[Cache] Marked block " + blockAddress + " as pending (completes cycle " + completionCycle + ")");
    }
    
    /**
     * Check if a block is currently being filled
     */
    public boolean isPending(int address) {
        int blockAddress = (address / blockSize) * blockSize;
        return pendingFills.containsKey(blockAddress);
    }

    /**
     * Simple write-through behavior: after memory is updated, refresh the cached block
     * so subsequent hits observe the new value.
     */
    public void writeThrough(int address, Memory memory) {
        int blockAddress = (address / blockSize) * blockSize;
        int index = (address / blockSize) % numBlocks;
        int tag = address / cacheSize;

        CacheLine line = lines[index];
        byte[] block = memory.loadBlock(blockAddress, blockSize);
        line.setValid(true);
        line.setTag(tag);
        line.setData(block);
        
        System.out.println("[Cache] Write-through at address " + address + " (index=" + index + ", tag=" + tag + ")");
    }
    
    /**
     * Pre-warm the cache with initial memory values.
     * Call this after memory initialization to ensure cache coherency.
     */
    public void warmCache(Memory memory, Map<Integer, Double> initialData) {
        if (initialData == null || initialData.isEmpty()) {
            return;
        }
        
        System.out.println("[Cache] Warming cache with initial memory values...");
        for (Integer address : initialData.keySet()) {
            int blockAddress = (address / blockSize) * blockSize;
            int index = (address / blockSize) % numBlocks;
            int tag = address / cacheSize;
            
            CacheLine line = lines[index];
            byte[] block = memory.loadBlock(blockAddress, blockSize);
            line.setValid(true);
            line.setTag(tag);
            line.setData(block);
            
            System.out.println("[Cache] Pre-loaded address " + address + " into cache (index=" + index + ", tag=" + tag + ")");
            debugPrintCacheLine(index, line);
        }
    }
    
    /**
     * Debug helper to print cache line contents
     */
    private void debugPrintCacheLine(int index, CacheLine line) {
        if (!line.isValid()) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("[Cache] Line ").append(index).append(" data: ");
        byte[] data = line.getData();
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        System.out.println(sb.toString());
        
        // Also print as doubles
        for (int offset = 0; offset + 7 < data.length; offset += 8) {
            long bits = 0;
            for (int j = 0; j < 8; j++) {
                long b = data[offset + j] & 0xFF;
                bits |= (b << (8 * j));
            }
            double value = Double.longBitsToDouble(bits);
            System.out.println("[Cache]   Offset " + offset + " as double: " + value);
        }
    }

    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public int getNumBlocks() { return numBlocks; }
    public CacheLine[] getLines() { return lines; }
    public int getBlockSize() { return blockSize; }
    
    public void clear() {
        hits = 0;
        misses = 0;
        pendingFills.clear();
        for (CacheLine line : lines) {
            line.setValid(false);
            line.setTag(0);
        }
    }

    public static class CacheLine {
        private boolean valid;
        private int tag;
        private byte[] data;

        CacheLine(int blockSize) {
            this.setValid(false);
            this.setTag(0);
            this.setData(new byte[blockSize]);
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public int getTag() {
            return tag;
        }

        public void setTag(int tag) {
            this.tag = tag;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    public static class CacheAccessResult {
        public final boolean hit;
        public final int latency;
        public final byte[] data;
        public final int blockAddress;

        public CacheAccessResult(boolean hit, int latency, byte[] data, int blockAddress) {
            this.hit = hit;
            this.latency = latency;
            this.data = data;
            this.blockAddress = blockAddress;
        }
    }
}