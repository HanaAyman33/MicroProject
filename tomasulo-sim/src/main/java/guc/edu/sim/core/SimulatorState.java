package guc.edu.sim.core;

import java.util.*;

/**
 * Complete Tomasulo simulator with all components integrated. 
 */
public class SimulatorState {

    private Program program;
    private IssueUnit issueUnit;
    private RegisterFile regFile;
    private Memory memory;
    private Cache cache;
    private RealReservationStations rs;
    private LoadBuffer loadBuffer;
    private StoreBuffer storeBuffer;
    private BranchUnit branchUnit;
    private Dispatcher dispatcher;
    private CommonDataBus cdb;
    private LatencyConfig latencyConfig;
    private final List<PendingResult> pendingResults = new ArrayList<>();
    
    private int lastIssuedIndex = -1;
    private List<InstructionStatus> instructionStatuses = new ArrayList<>();
    
    // Configuration
    private int fpAddSize = 3;
    private int fpMulSize = 2;
    private int intSize = 2;
    private int loadBufferSize = 3;
    private int storeBufferSize = 3;
    private int cacheSize = 64;
    private int blockSize = 16;
    private int cacheHitLatency = 1;
    private int cacheMissPenalty = 10;
    private int fpAddLatency = 3;
    private int fpMulLatency = 10;
    private int intLatency = 1;
    private int loadLatency = 2;
    private int storeLatency = 2;
    private int branchLatency = 1;
    private int rawHazards;
    private int warHazards;
    private int wawHazards;
    private int structuralHazards;
    private int loadIssued;
    private int storeIssued;
    private int fpIssued;
    private int intIssued;
    private int branchIssued;
    private Map<String, Double> initialRegValues = new HashMap<>();
    private Map<Integer, Double> initialMemValues = new HashMap<>();
    private final List<IssuedInstructionInfo> inFlight = new ArrayList<>();
    private final Map<String, IssuedInstructionInfo> tagToInstruction = new HashMap<>();
    private int branchTagCounter = 0;
    private String activeBranchTag;

    public void loadProgramLines(List<String> lines) {
        ProgramLoader loader = new ProgramLoader();
        this.program = loader.loadFromLines(lines);
        initializeSimulator();
    }

    private void initializeSimulator() {
        System.out.println("\n========== Initializing Tomasulo Simulator ==========");
        
        // Create components
        latencyConfig = new LatencyConfig();
        latencyConfig.setLatency(StationType.FP_ADD, fpAddLatency);
        latencyConfig.setLatency(StationType.FP_MUL, fpMulLatency);
        latencyConfig.setLatency(StationType.INTEGER, intLatency);
        latencyConfig.setLatency(StationType.LOAD, loadLatency);
        latencyConfig.setLatency(StationType.STORE, storeLatency);
        regFile = new RegisterFile();
        memory = new Memory();
        cache = new Cache(cacheSize, blockSize, cacheHitLatency, cacheMissPenalty);
        
        rs = new RealReservationStations(fpAddSize, fpMulSize, intSize, regFile);
        
        // Create separate load and store buffers
        loadBuffer = new LoadBuffer(loadBufferSize, regFile, memory, cache);
        storeBuffer = new StoreBuffer(storeBufferSize, regFile, memory, cache);
        
        branchUnit = new BranchUnit(regFile, program);
        branchUnit.setLatency(branchLatency);
        
        dispatcher = new Dispatcher(latencyConfig);
        dispatcher.addExecutionUnit(StationType.FP_ADD, 2);
        dispatcher.addExecutionUnit(StationType.FP_MUL, 1);
        dispatcher.addExecutionUnit(StationType.INTEGER, 1);
        
        cdb = new CommonDataBus();
        cdb.addListener((tag, result) -> {
            System.out.println("[CDB] Broadcasting " + tag + " = " + result);
            rs.broadcastResult(tag, result);
            loadBuffer.broadcastResult(tag, result);
            storeBuffer. broadcastResult(tag, result);
            branchUnit.broadcastResult(tag, result);
            
            // Update register file
            for (String reg : regFile.getAllProducers(). keySet()) {
                if (tag.equals(regFile.getProducer(reg))) {
                    regFile.setValue(reg, result);
                    regFile.clearProducer(reg);
                }
            }
        });
        
        issueUnit = new IssueUnit(program);
        
        // Initialize instruction statuses
        instructionStatuses.clear();
        for (int i = 0; i < program.size(); i++) {
            instructionStatuses.add(new InstructionStatus());
        }
        pendingResults.clear();
        inFlight.clear();
        tagToInstruction.clear();
        rawHazards = warHazards = wawHazards = structuralHazards = 0;
        loadIssued = storeIssued = fpIssued = intIssued = branchIssued = 0;
        branchTagCounter = 0;
        activeBranchTag = null;

        // Reapply user-specified initial values if present
        if (!initialRegValues.isEmpty()) {
            regFile.loadInitialValues(initialRegValues);
            System.out.println("[Init] Re-applied register values: " + initialRegValues);
        }
        if (!initialMemValues.isEmpty()) {
            memory.loadInitialData(initialMemValues);
            System.out.println("[Init] Re-applied memory values: " + initialMemValues);
        }
        
        SimulationClock.reset();
        this.lastIssuedIndex = -1;
        
        System. out.println("========== Initialization Complete ==========\n");
    }

    public boolean isProgramLoaded() {
        return program != null;
    }

    public boolean step() {
        if (program == null || issueUnit == null) return false;
        
        int currentCycle = SimulationClock.getCycle() + 1;
        System.out.println("\n========== Cycle " + currentCycle + " ==========");

        // Phase 0: Write-back any results that finished in the previous cycle
        if (!pendingResults.isEmpty()) {
            List<PendingResult> toBroadcast = new ArrayList<>(pendingResults);
            pendingResults.clear();
            for (PendingResult pr : toBroadcast) {
                if (pr.broadcast) {
                    cdb.broadcast(pr.tag, pr.result);
                }
                // Results that were produced in the previous simulator step are
                // considered to write back in the cycle immediately after their
                // last execution cycle. Since we are at currentCycle, that
                // write-back cycle is (currentCycle - 1).
                markInstructionWriteBack(pr.tag, currentCycle - 1);
            }
        }
        
        // Phase 1: Collect completed execution-unit results
        List<ReservationStationEntry> finishedRS = dispatcher.tickUnits();
        for (ReservationStationEntry entry : finishedRS) {
            System.out.println("[WriteBack] " + entry. getId() + " completed execution");
            double result = (entry.getResult() instanceof Double) ?
                (Double) entry.getResult() : 0.0;

            // Record exec end as the *last cycle of execution*.
            // The unit finishes at the beginning of currentCycle, so the last
            // active execution cycle is (currentCycle - 1).
            markInstructionExecEnd(entry.getId(), currentCycle - 1);

            // Defer broadcast/write-back to the next simulator step so that
            // dependent instructions only see the value starting in the
            // *next* cycle. The reservation-station slot will be freed when
            // the result is broadcast (see RealReservationStations).
            pendingResults.add(new PendingResult(entry.getId(), result, true));
        }
        
        // Phase 2: Execute LOAD operations
        List<LoadBuffer.LoadEntry> completedLoads = new ArrayList<>();
        for (LoadBuffer.LoadEntry loadEntry : loadBuffer.getBuffer()) {
            if (loadEntry.executing) {
                loadEntry.remainingCycles--;
                System.out.println("[LoadBuffer] " + loadEntry.tag + " executing...  " + 
                                 loadEntry.remainingCycles + " cycles remaining");
                
                if (loadEntry.remainingCycles <= 0) {
                    System.out.println("[LoadBuffer] " + loadEntry.tag + " COMPLETED with value " + loadEntry.result);
                    // Load completes at the beginning of this cycle, so its last
                    // execution cycle is (currentCycle - 1).
                    markInstructionExecEnd(loadEntry.tag, currentCycle - 1);

                    // Defer broadcast/write-back to the next simulator step so
                    // that dependents observe the result one cycle later.
                    pendingResults.add(new PendingResult(loadEntry.tag, loadEntry.result, true));
                    completedLoads.add(loadEntry);
                }
            } else if (loadEntry.isReady()) {
                // Start executing this load in the *next* cycle after it becomes ready.
                // We set remainingCycles to the cache/memory latency but do not tick it
                // down in the same simulator step, so an N-cycle load occupies cycles
                // [execStart, execStart + N - 1].
                loadEntry.executing = true;
                int addr = loadEntry.computeAddress();

                Cache.CacheAccessResult result = cache.access(addr, memory);
                loadEntry.remainingCycles = result.latency;
                loadEntry.result = loadFromMemory(loadEntry.instruction, addr);
                System.out.println("[LoadBuffer] " + loadEntry.tag + " LOADING from address " + addr +
                                 " (latency=" + result.latency + " cycles)");
                markInstructionExecStart(loadEntry.tag, currentCycle);
            }
        }
        
        // Remove completed load entries
        for (LoadBuffer. LoadEntry entry : completedLoads) {
            loadBuffer.removeEntry(entry);
        }
        
        // Phase 3: Execute STORE operations
        List<StoreBuffer.StoreEntry> completedStores = new ArrayList<>();
        for (StoreBuffer. StoreEntry storeEntry : storeBuffer.getBuffer()) {
            if (storeEntry.executing) {
                storeEntry.remainingCycles--;
                System.out.println("[StoreBuffer] " + storeEntry.tag + " executing... " + 
                                 storeEntry.remainingCycles + " cycles remaining");
                
                if (storeEntry.remainingCycles <= 0) {
                    System.out.println("[StoreBuffer] " + storeEntry. tag + " COMPLETED");
                    // Store completes at the beginning of this cycle, so its last
                    // execution cycle is (currentCycle - 1).
                    markInstructionExecEnd(storeEntry.tag, currentCycle - 1);

                    // Stores don't broadcast on the CDB, but we still record their
                    // write-back via the pendingResults queue in the *next* step.
                    pendingResults.add(new PendingResult(storeEntry.tag, storeEntry.storeValue, false));
                    completedStores.add(storeEntry);
                }
            } else if (storeEntry.isReady()) {
                // Start executing this store; like loads, we only begin counting
                // latency from the *next* cycle after execStart.
                storeEntry.executing = true;
                int addr = storeEntry.computeAddress();

                Cache.CacheAccessResult result = cache.access(addr, memory);
                storeEntry.remainingCycles = result.latency;
                storeToMemory(storeEntry.instruction, addr, storeEntry.storeValue);
                cache.writeThrough(addr, memory);
                System.out.println("[StoreBuffer] " + storeEntry.tag + " STORING " + storeEntry.storeValue +
                                 " to address " + addr);
                markInstructionExecStart(storeEntry.tag, currentCycle);
            }
        }
        
        // Remove completed store entries
        for (StoreBuffer.StoreEntry entry : completedStores) {
            storeBuffer.removeEntry(entry);
        }
        
        // Phase 4: Dispatch ready instructions to execution units
        for (ReservationStationEntry entry : rs.getStations()) {
            if (entry.isReady() && !entry.isExecuting()) {
                dispatcher.addEntry(entry);
                System.out.println("[Dispatch] Added " + entry.getId() + " to dispatcher queue");
            }
        }
        dispatcher.dispatch();
        
        // Mark execution start for newly dispatched instructions
        for (ReservationStationEntry entry : rs.getStations()) {
            if (entry.isExecuting()) {
                markInstructionExecStart(entry.getId(), currentCycle);
            }
        }
        
        // Phase 5: Resolve branches
        branchUnit.tryResolve();
        if (branchUnit.hasResolvedBranch()) {
            if (activeBranchTag != null) {
                // Branch resolves at the beginning of this cycle, so its last
                // execution cycle is (currentCycle - 1).
                markInstructionExecEnd(activeBranchTag, currentCycle - 1);
                markInstructionWriteBack(activeBranchTag, currentCycle);
                activeBranchTag = null;
            }
            if (branchUnit.shouldFlushQueue()) {
                int targetPc = branchUnit.getResolvedTargetPc();
                System.out. println("[Branch] Taking branch to PC=" + targetPc);
                issueUnit.jumpTo(targetPc);
            }
            branchUnit.clear();
        }
        
        // Phase 6: Issue new instruction
        int prevPc = issueUnit.getPc();
        boolean issued = false;
        String assignedTag = null;
        HazardSnapshot hazardSnapshot = null;
        
        if (issueUnit.hasNext()) {
            Instruction instr = program.get(issueUnit.getPc());
            
            boolean canIssue = false;
            switch (instr.getType()) {
                case ALU_FP:
                case ALU_INT:
                    canIssue = rs.hasFreeFor(instr);
                    if (canIssue) {
                        hazardSnapshot = detectHazards(instr);
                        rs.accept(instr, null);
                        assignedTag = rs.getLastAllocatedTag();
                        System.out.println("[Issue] Issued to RS: " + instr.getOpcode() + " -> " + assignedTag);
                    } else {
                        structuralHazards++;
                    }
                    break;
                    
                case LOAD:
                    canIssue = loadBuffer.hasFree();
                    if (canIssue) {
                        hazardSnapshot = detectHazards(instr);
                        loadBuffer.accept(instr);
                        assignedTag = loadBuffer.getLastAllocatedTag();
                        System.out.println("[Issue] Issued to Load Buffer: " + instr.getOpcode() + " -> " + assignedTag);
                    } else {
                        structuralHazards++;
                    }
                    break;
                    
                case STORE:
                    canIssue = storeBuffer.hasFree();
                    if (canIssue) {
                        hazardSnapshot = detectHazards(instr);
                        storeBuffer.accept(instr);
                        assignedTag = storeBuffer.getLastAllocatedTag();
                        System.out.println("[Issue] Issued to Store Buffer: " + instr.getOpcode() + " -> " + assignedTag);
                    } else {
                        structuralHazards++;
                    }
                    break;
                    
                case BRANCH:
                    canIssue = branchUnit.isFree();
                    if (canIssue) {
                        hazardSnapshot = detectHazards(instr);
                        branchUnit.accept(instr, null);
                        assignedTag = "BR" + (++branchTagCounter);
                        activeBranchTag = assignedTag;
                        markInstructionExecStart(assignedTag, currentCycle);
                        System.out.println("[Issue] Issued to Branch Unit: " + instr.getOpcode());
                    } else {
                        structuralHazards++;
                    }
                    break;
                case UNKNOWN:
                default:
                    System.out.println("[Issue] Unsupported instruction type: " + instr.getOpcode());
                    break;
            }
            
            if (canIssue) {
                instr.setIssueCycle(currentCycle);
                instructionStatuses.get(prevPc).issueCycle = currentCycle;
                instructionStatuses.get(prevPc).tag = assignedTag;  // FIXED: Store the tag
                issueUnit.jumpTo(prevPc + 1);
                recordInstructionMix(instr);
                if (hazardSnapshot != null) {
                    if (hazardSnapshot.raw) rawHazards++;
                    if (hazardSnapshot.war) warHazards++;
                    if (hazardSnapshot.waw) wawHazards++;
                }
                trackIssuedInstruction(instr, assignedTag);
                issued = true;
                lastIssuedIndex = prevPc;
                System.out.println("[Issue] PC advanced from " + prevPc + " to " + issueUnit.getPc());
            } else {
                System.out. println("[Issue] STALLED - No free resources for " + instr.getOpcode());
            }
        }
        
        // Advance clock
        SimulationClock. nextCycle();
        
        // Print status
        printStatus();
        
        return issued;
    }
    
    private void markInstructionExecStart(String tag, int cycle) {
        for (int i = 0; i < instructionStatuses.size(); i++) {
            InstructionStatus status = instructionStatuses.get(i);
            if (status.tag != null && status.tag.equals(tag) && status.execStartCycle == -1) {
                status.execStartCycle = cycle;
                break;
            }
        }
    }
    
    private void markInstructionExecEnd(String tag, int cycle) {
        for (int i = 0; i < instructionStatuses. size(); i++) {
            InstructionStatus status = instructionStatuses.get(i);
            if (status.tag != null && status.tag.equals(tag) && status.execEndCycle == -1) {
                status. execEndCycle = cycle;
                break;
            }
        }
    }
    
    private void markInstructionWriteBack(String tag, int cycle) {
        for (int i = 0; i < instructionStatuses.size(); i++) {
            InstructionStatus status = instructionStatuses.get(i);
            if (status.tag != null && status.tag.equals(tag) && status.writeBackCycle == -1) {
                status.writeBackCycle = cycle;
                break;
            }
        }
        completeIssuedInstruction(tag);
    }
    
    private void printStatus() {
        System.out.println("\n--- Current State ---");
        System.out.println("Reservation Stations: " + rs.getStations().size());
        for (ReservationStationEntry entry : rs.getStations()) {
            System.out.println("  " + entry);
        }
        
        System.out.println("Load Buffer: " + loadBuffer.getBuffer().size());
        for (LoadBuffer.LoadEntry entry : loadBuffer.getBuffer()) {
            System.out.println("  " + entry);
        }
        
        System.out.println("Store Buffer: " + storeBuffer.getBuffer().size());
        for (StoreBuffer.StoreEntry entry : storeBuffer.getBuffer()) {
            System.out.println("  " + entry);
        }
        
        System.out.println("Cache Hits: " + cache.getHits() + ", Misses: " + cache.getMisses());
        System.out.println("--------------------\n");
    }

    private double loadFromMemory(Instruction instr, int address) {
        String op = instr.getOpcode().toUpperCase();
        switch (op) {
            case "LW":
                return memory.loadWord(address);
            case "L.S":
                return memory.loadFloat(address);
            case "LD":
            case "L.D":
            default:
                return memory.loadDouble(address);
        }
    }

    private void storeToMemory(Instruction instr, int address, double value) {
        String op = instr.getOpcode().toUpperCase();
        switch (op) {
            case "SW":
                memory.storeWord(address, (int) value);
                break;
            case "S.S":
                memory.storeFloat(address, (float) value);
                break;
            case "SD":
            case "S.D":
            default:
                memory.storeDouble(address, value);
                break;
        }
    }

    public void reset() {
        SimulationClock.reset();
        lastIssuedIndex = -1;
        if (issueUnit != null) issueUnit.jumpTo(0);
        if (cache != null) cache. clear();
        if (program != null) {
            initializeSimulator();
        }
    }

    public int getCycle() { return SimulationClock.getCycle(); }
    public Program getProgram() { return program; }
    public int getLastIssuedIndex() { return lastIssuedIndex; }
    public IssueUnit getIssueUnit() { return issueUnit; }
    public RegisterFile getRegFile() { return regFile; }
    public Cache getCache() { return cache; }
    public RealReservationStations getReservationStations() { return rs; }
    public LoadBuffer getLoadBuffer() { return loadBuffer; }
    public StoreBuffer getStoreBuffer() { return storeBuffer; }
    public List<InstructionStatus> getInstructionStatuses() { return instructionStatuses; }
    public int getFpAddSize() { return fpAddSize; }
    public int getFpMulSize() { return fpMulSize; }
    public int getIntSize() { return intSize; }
    public int getLoadBufferSize() { return loadBufferSize; }
    public int getStoreBufferSize() { return storeBufferSize; }
    
    public void setConfiguration(int fpAdd, int fpMul, int intAlu, 
                                 int loadBufSize, int storeBufSize,
                                 int cacheSz, int blockSz, int hitLat, int missPen) {
        this.fpAddSize = fpAdd;
        this.fpMulSize = fpMul;
        this.intSize = intAlu;
        this.loadBufferSize = loadBufSize;
        this.storeBufferSize = storeBufSize;
        this.cacheSize = cacheSz;
        this. blockSize = blockSz;
        this. cacheHitLatency = hitLat;
        this.cacheMissPenalty = missPen;

        // Rebuild the simulator with the new configuration if a program is loaded
        if (program != null) {
            initializeSimulator();
        }
    }

    public void setConfigurationWithLatencies(int fpAdd, int fpMul, int intAlu,
                                              int loadBufSize, int storeBufSize,
                                              int cacheSz, int blockSz, int hitLat, int missPen,
                                              int fpAddLat, int fpMulLat, int intLat,
                                              int loadLat, int storeLat, int branchLat) {
        this.fpAddSize = fpAdd;
        this.fpMulSize = fpMul;
        this.intSize = intAlu;
        this.loadBufferSize = loadBufSize;
        this.storeBufferSize = storeBufSize;
        this.cacheSize = cacheSz;
        this.blockSize = blockSz;
        this.cacheHitLatency = hitLat;
        this.cacheMissPenalty = missPen;
        this.fpAddLatency = fpAddLat;
        this.fpMulLatency = fpMulLat;
        this.intLatency = intLat;
        this.loadLatency = loadLat;
        this.storeLatency = storeLat;
        this.branchLatency = branchLat;

        if (program != null) {
            initializeSimulator();
        }
    }
    
    public void loadInitialRegisterValues(Map<String, Double> values) {
        initialRegValues = new HashMap<>(values);
        if (regFile != null) {
            regFile.loadInitialValues(initialRegValues);
            System.out.println("[Init] Loaded initial register values: " + initialRegValues);
        }
    }
    
    public void loadInitialMemoryValues(Map<Integer, Double> values) {
        initialMemValues = new HashMap<>(values);
        if (memory != null) {
            memory.loadInitialData(initialMemValues);
            System.out.println("[Init] Loaded initial memory values: " + initialMemValues);
        }
    }

    public int getRawHazards() { return rawHazards; }
    public int getWarHazards() { return warHazards; }
    public int getWawHazards() { return wawHazards; }
    public int getStructuralHazards() { return structuralHazards; }
    public int getLoadIssuedCount() { return loadIssued; }
    public int getStoreIssuedCount() { return storeIssued; }
    public int getFpIssuedCount() { return fpIssued; }
    public int getIntIssuedCount() { return intIssued; }
    public int getBranchIssuedCount() { return branchIssued; }

    private void recordInstructionMix(Instruction instr) {
        switch (instr.getType()) {
            case LOAD:
                loadIssued++;
                break;
            case STORE:
                storeIssued++;
                break;
            case ALU_FP:
                fpIssued++;
                break;
            case ALU_INT:
                intIssued++;
                break;
            case BRANCH:
                branchIssued++;
                break;
            default:
                break;
        }
    }

    private void trackIssuedInstruction(Instruction instr, String tag) {
        List<String> sources = extractSourceRegisters(instr);
        String dest = extractDestinationRegister(instr);
        IssuedInstructionInfo info = new IssuedInstructionInfo(tag, dest, sources);
        inFlight.add(info);
        if (tag != null) {
            tagToInstruction.put(tag, info);
        }
    }

    private void completeIssuedInstruction(String tag) {
        if (tag == null) return;
        IssuedInstructionInfo info = tagToInstruction.remove(tag);
        if (info != null) {
            info.completed = true;
            inFlight.remove(info);
        }
    }

    private HazardSnapshot detectHazards(Instruction instr) {
        List<String> sources = extractSourceRegisters(instr);
        String dest = extractDestinationRegister(instr);
        boolean raw = false;
        boolean war = false;
        boolean waw = false;

        for (IssuedInstructionInfo info : inFlight) {
            if (info.completed) continue;
            if (!raw && info.dest != null) {
                for (String src : sources) {
                    if (src != null && src.equals(info.dest)) {
                        raw = true;
                        break;
                    }
                }
            }
            if (!war && dest != null) {
                for (String src : info.sources) {
                    if (dest.equals(src)) {
                        war = true;
                        break;
                    }
                }
            }
            if (!waw && dest != null && dest.equals(info.dest)) {
                waw = true;
            }
        }
        return new HazardSnapshot(raw, war, waw);
    }

    private List<String> extractSourceRegisters(Instruction instr) {
        List<String> sources = new ArrayList<>();
        if (instr == null) return sources;
        switch (instr.getType()) {
            case LOAD:
                if (isRegister(instr.getBase())) sources.add(instr.getBase());
                break;
            case STORE:
                if (isRegister(instr.getSrc1())) sources.add(instr.getSrc1());
                if (isRegister(instr.getBase())) sources.add(instr.getBase());
                break;
            case ALU_FP:
            case ALU_INT:
                if (isRegister(instr.getSrc1())) sources.add(instr.getSrc1());
                if (isRegister(instr.getSrc2())) sources.add(instr.getSrc2());
                break;
            case BRANCH:
                if (isRegister(instr.getSrc1())) sources.add(instr.getSrc1());
                if (isRegister(instr.getSrc2())) sources.add(instr.getSrc2());
                break;
            default:
                break;
        }
        return sources;
    }

    private String extractDestinationRegister(Instruction instr) {
        if (instr == null) return null;
        switch (instr.getType()) {
            case LOAD:
            case ALU_FP:
            case ALU_INT:
                return instr.getDest();
            default:
                return null;
        }
    }

    private boolean isRegister(String operand) {
        if (operand == null) return false;
        return !operand.matches("-?\\d+");
    }

    private static class IssuedInstructionInfo {
        @SuppressWarnings("unused")
        final String tag;
        final String dest;
        final List<String> sources;
        boolean completed = false;

        IssuedInstructionInfo(String tag, String dest, List<String> sources) {
            this.tag = tag;
            this.dest = dest;
            this.sources = sources;
        }
    }

    private static class HazardSnapshot {
        final boolean raw;
        final boolean war;
        final boolean waw;

        HazardSnapshot(boolean raw, boolean war, boolean waw) {
            this.raw = raw;
            this.war = war;
            this.waw = waw;
        }
    }
    
    public static class InstructionStatus {
        public String tag;
        public int issueCycle = -1;
        public int execStartCycle = -1;
        public int execEndCycle = -1;
        public int writeBackCycle = -1;
    }

    private static class PendingResult {
        final String tag;
        final double result;
        final boolean broadcast;

        PendingResult(String tag, double result, boolean broadcast) {
            this.tag = tag;
            this.result = result;
            this.broadcast = broadcast;
        }
    }
}
