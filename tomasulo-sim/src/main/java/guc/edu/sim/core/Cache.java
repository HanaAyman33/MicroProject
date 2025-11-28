package guc. edu.sim.core;

import java.util. HashMap;
import java.util. Map;

/**
 * Direct-mapped cache implementation.
 * Supports configurable cache size and block size.
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

    public CacheAccessResult access(int address, Memory memory) {
        int blockAddress = (address / blockSize) * blockSize;
        int index = (address / blockSize) % numBlocks;
        int tag = address / cacheSize;
        
        CacheLine line = lines[index];
        
        if (line.isValid() && line.getTag() == tag) {
            // Cache hit
            hits++;
            System.out.println("[Cache] HIT at address " + address + " (index=" + index + ", tag=" + tag + ")");
            return new CacheAccessResult(true, hitLatency, line.getData());
        } else {
            // Cache miss
            misses++;
            System.out. println("[Cache] MISS at address " + address + " (index=" + index + ", tag=" + tag + ")");
            
            // Load block from memory
            byte[] block = memory.loadBlock(blockAddress, blockSize);
            line.setValid(true);
            line.setTag(tag);
            line.setData(block);
            
            return new CacheAccessResult(false, hitLatency + missPenalty, block);
        }
    }

    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public int getNumBlocks() { return numBlocks; }
    public CacheLine[] getLines() { return lines; }
    
    public void clear() {
        hits = 0;
        misses = 0;
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

        public CacheAccessResult(boolean hit, int latency, byte[] data) {
            this.hit = hit;
            this.latency = latency;
            this.data = data;
        }
    }
}