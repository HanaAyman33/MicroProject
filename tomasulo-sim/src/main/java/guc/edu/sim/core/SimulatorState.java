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
    private int fpDivLatency = 40;
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
    private int currentBroadcastCycle = -1;
    
    // DEBUG: Track specific instructions
    private static final boolean DEBUG = true;
    private void debug(String msg) {
        if (DEBUG) System.out.println("[DEBUG] " + msg);
    }

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
        latencyConfig.setDivisionLatency(fpDivLatency);
        latencyConfig.setLatency(StationType.INTEGER, intLatency);
        latencyConfig.setLatency(StationType.LOAD, loadLatency);
        latencyConfig.setLatency(StationType.STORE, storeLatency);
        
        regFile = new RegisterFile();
        memory = new Memory();
        cache = new Cache(cacheSize, blockSize, cacheHitLatency, cacheMissPenalty);
        
        rs = new RealReservationStations(fpAddSize, fpMulSize, intSize, regFile);
        
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
            rs.broadcastResult(tag, result, currentBroadcastCycle);
            loadBuffer.broadcastResult(tag, result, currentBroadcastCycle);
            storeBuffer.broadcastResult(tag, result, currentBroadcastCycle);
            branchUnit.broadcastResult(tag, result, currentBroadcastCycle);
            
            for (String reg : regFile.getAllProducers().keySet()) {
                if (tag.equals(regFile.getProducer(reg))) {
                    regFile.setValue(reg, result);
                    regFile.clearProducer(reg);
                }
            }
        });
        
        issueUnit = new IssueUnit(program);
        
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
        
        System.out.println("========== Initialization Complete ==========\n");
    }

    public boolean isProgramLoaded() {
        return program != null;
    }

    public boolean step() {
        if (program == null || issueUnit == null) return false;
        
        int currentCycle = SimulationClock.getCycle() + 1;
        System.out.println("\n========== Cycle " + currentCycle + " ==========");
        
        debug("=== CYCLE " + currentCycle + " START ===");
        debug("pendingResults at start: " + pendingResults.size());
        for (PendingResult pr : pendingResults) {
            debug("  - " + pr.tag + " (broadcast=" + pr.broadcast + ")");
        }

        // Phase 0: Write-back any results that finished in the previous cycle
        currentBroadcastCycle = currentCycle;
        if (!pendingResults.isEmpty()) {
            debug("PHASE 0: Processing " + pendingResults.size() + " pending results");
            PendingResult broadcastThisCycle = null;
            int deferredCount = 0;
            Iterator<PendingResult> iterator = pendingResults.iterator();
            
            while (iterator.hasNext()) {
                PendingResult pr = iterator.next();
                debug("Processing: " + pr.tag + " (broadcast=" + pr.broadcast + ")");
                
                if (!pr.broadcast) {
                    iterator.remove();
                    markInstructionWriteBack(pr.tag, currentCycle);
                    debug("Non-broadcast write-back for " + pr.tag);
                    continue;
                }
                
                if (broadcastThisCycle == null) {
                    broadcastThisCycle = pr;
                    iterator.remove();
                    markInstructionWriteBack(pr.tag, currentCycle); // FIXED: Mark write-back here too
                    debug("Selected for CDB broadcast: " + pr.tag);
                } else {
                    deferredCount++;
                    debug("Deferred (CDB busy): " + pr.tag);
                }
            }
            
            if (broadcastThisCycle != null) {
                debug("CDB Broadcasting: " + broadcastThisCycle.tag + " = " + broadcastThisCycle.result);
                cdb.broadcast(broadcastThisCycle.tag, broadcastThisCycle.result);
                debug("Write-back already marked for " + broadcastThisCycle.tag);
                
                if (deferredCount > 0) {
                    System.out.println("[CDB] Bus busy; deferred " + deferredCount + " result(s) to later cycle(s)");
                }
            }
        } else {
            debug("No pending results to write back");
        }
        
        // Phase 1: Tick execution units (instructions that were already executing)
        debug("PHASE 1: Checking dispatcher for completed instructions");
        List<ReservationStationEntry> finishedRS = dispatcher.tickUnits();
        debug("Dispatcher returned " + finishedRS.size() + " finished entries");
        for (ReservationStationEntry entry : finishedRS) {
            debug("Finished: " + entry.getId() + " op=" + entry.getOpcode());
            double result = (entry.getResult() instanceof Double) ? 
                (Double) entry.getResult() : 0.0;

            markInstructionExecEnd(entry.getId(), currentCycle);
            debug("Marked exec end for " + entry.getId());

            pendingResults.add(new PendingResult(entry.getId(), result, true));
            debug("Added to pendingResults: " + entry.getId() + " (size now=" + pendingResults.size() + ")");
        }
        
        // Phase 2: Track previously executing instructions
        Set<String> previouslyExecuting = new HashSet<>();
        for (ReservationStationEntry entry : rs.getStations()) {
            if (entry.isExecuting()) {
                previouslyExecuting.add(entry.getId());
                debug("Already executing: " + entry.getId() + " (hasExecutedForCycles=" + entry.hasExecutedForCycles() + ")");
            }
        }
        
        // Phase 3: Dispatch ready instructions to execution units
        debug("PHASE 3: Dispatching ready instructions");
        for (ReservationStationEntry entry : rs.getStations()) {
            if (entry.isReadyForDispatch(currentCycle) && !entry.isExecuting()) {
                dispatcher.addEntry(entry);
                debug("Dispatched to execution unit: " + entry.getId());
            }
        }
        
        dispatcher.dispatch();
        
        // Phase 4: Check for newly started latency-1 instructions
        debug("PHASE 4: Checking for newly started instructions (latency-1 detection)");
        boolean foundLatency1 = false;
        for (ReservationStationEntry entry : rs.getStations()) {
            if (entry.isExecuting() && !previouslyExecuting.contains(entry.getId())) {
                debug("Newly started executing: " + entry.getId() + " op=" + entry.getOpcode());
                markInstructionExecStart(entry.getId(), currentCycle);
                debug("Marked exec start for " + entry.getId());
                
                // Check if it's a latency-1 instruction
                int latency = getInstructionLatency(entry);
                debug("Latency for " + entry.getId() + " = " + latency);
                
                if (latency == 1) {
                    foundLatency1 = true;
                    debug("LATENCY-1 DETECTED for " + entry.getId());
                    markInstructionExecEnd(entry.getId(), currentCycle);
                    debug("Marked exec end for " + entry.getId() + " (same cycle)");
                    
                    double result = computeResult(entry);
                    debug("Computed result for " + entry.getId() + " = " + result);
                    
                    pendingResults.add(new PendingResult(entry.getId(), result, true));
                    debug("Added to pendingResults: " + entry.getId() + " (size now=" + pendingResults.size() + ")");
                    
                    entry.markCompleted();
                    debug("Marked as completed: " + entry.getId());
                }
            }
        }
        if (!foundLatency1) {
            debug("No latency-1 instructions detected in this cycle");
        }
        
        // Phase 5: Tick LOAD operations that are already executing
        List<LoadBuffer.LoadEntry> completedLoads = new ArrayList<>();
        for (LoadBuffer.LoadEntry loadEntry : loadBuffer.getBuffer()) {
            if (loadEntry.executing) {
                loadEntry.remainingCycles--;
                
                if (loadEntry.remainingCycles == 0) {
                    System.out.println("[LoadBuffer] " + loadEntry.tag + " COMPLETED with value " + loadEntry.result);
                    markInstructionExecEnd(loadEntry.tag, currentCycle);
                    pendingResults.add(new PendingResult(loadEntry.tag, loadEntry.result, true));
                    completedLoads.add(loadEntry);
                }
            }
        }
        for (LoadBuffer.LoadEntry entry : completedLoads) {
            loadBuffer.removeEntry(entry);
        }

        // Phase 6: Tick STORE operations that are already executing
        List<StoreBuffer.StoreEntry> completedStores = new ArrayList<>();
        for (StoreBuffer.StoreEntry storeEntry : storeBuffer.getBuffer()) {
            if (storeEntry.executing) {
                storeEntry.remainingCycles--;
                
                if (storeEntry.remainingCycles == 0) {
                    System.out.println("[StoreBuffer] " + storeEntry.tag + " COMPLETED");
                    markInstructionExecEnd(storeEntry.tag, currentCycle);
                    pendingResults.add(new PendingResult(storeEntry.tag, storeEntry.storeValue, false));
                    completedStores.add(storeEntry);
                }
            }
        }
        for (StoreBuffer.StoreEntry entry : completedStores) {
            storeBuffer.removeEntry(entry);
        }
        
        // Phase 7: Start NEW load operations that are ready
        for (LoadBuffer.LoadEntry loadEntry : loadBuffer.getBuffer()) {
            if (!loadEntry.executing && loadEntry.isReadyForDispatch(currentCycle)) {
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

        // Phase 8: Start NEW store operations that are ready
        for (StoreBuffer.StoreEntry storeEntry : storeBuffer.getBuffer()) {
            if (!storeEntry.executing && storeEntry.isReadyForDispatch(currentCycle)) {
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
        
        // Phase 9: Resolve branches
        branchUnit.tryResolve(currentCycle);
        if (branchUnit.hasResolvedBranch()) {
            if (activeBranchTag != null) {
                markInstructionExecEnd(activeBranchTag, currentCycle);
                markInstructionWriteBack(activeBranchTag, currentCycle);
                activeBranchTag = null;
            }
            if (branchUnit.shouldFlushQueue()) {
                int targetPc = branchUnit.getResolvedTargetPc();
                System.out.println("[Branch] Taking branch to PC=" + targetPc);
                issueUnit.jumpTo(targetPc);
            }
            branchUnit.clear();
        }
        
        // Phase 10: Issue new instruction
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
                        debug("Instruction issued: " + instr.getOpcode() + " tag=" + assignedTag);
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
                instructionStatuses.get(prevPc).tag = assignedTag; // FIXED: Store the tag
                debug("Stored tag " + assignedTag + " for instruction at index " + prevPc);
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
                System.out.println("[Issue] STALLED - No free resources for " + instr.getOpcode());
            }
        }
        
        // Advance clock
        SimulationClock.nextCycle();
        
        debug("=== END CYCLE " + currentCycle + " ===");
        debug("pendingResults at end: " + pendingResults.size());
        for (PendingResult pr : pendingResults) {
            debug("  - " + pr.tag + " (broadcast=" + pr.broadcast + ")");
        }
        
        // Print status
        printStatus();
        
        return issued;
    }
    
    // Helper method to get instruction latency
    private int getInstructionLatency(ReservationStationEntry entry) {
        if (entry == null) {
            debug("getInstructionLatency: entry is null");
            return 1;
        }
        
        Instruction instr = entry.getInstruction();
        if (instr == null) {
            debug("getInstructionLatency: instruction is null for entry " + entry.getId());
            debug("Entry opcode: " + entry.getOpcode());
            return 1;
        }
        
        String opcode = instr.getOpcode().toUpperCase();
        debug("getInstructionLatency for " + opcode + " type=" + instr.getType());
        
        switch (instr.getType()) {
            case ALU_INT:
                debug("ALU_INT latency = " + intLatency);
                return intLatency;  // Should be 1
            case ALU_FP:
                if (opcode.contains("ADD") || opcode.contains("SUB")) {
                    debug("FP ADD/SUB latency = " + fpAddLatency);
                    return fpAddLatency;
                } else if (opcode.contains("MUL")) {
                    debug("FP MUL latency = " + fpMulLatency);
                    return fpMulLatency;
                } else if (opcode.contains("DIV")) {
                    debug("FP DIV latency = " + fpDivLatency);
                    return fpDivLatency;
                }
                debug("Unknown FP opcode, default latency = 1");
                return 1;
            case LOAD:
                debug("LOAD latency = " + loadLatency);
                return loadLatency;
            case STORE:
                debug("STORE latency = " + storeLatency);
                return storeLatency;
            case BRANCH:
                debug("BRANCH latency = " + branchLatency);
                return branchLatency;
            default:
                debug("Unknown type, default latency = 1");
                return 1;
        }
    }
    
    // Helper method to compute instruction result
    private double computeResult(ReservationStationEntry entry) {
        debug("computeResult for " + entry.getId() + " op=" + entry.getOpcode());
        
        if (entry.getVj() != null && entry.getVk() != null) {
            try {
                double vj = Double.parseDouble(entry.getVj().toString());
                double vk = Double.parseDouble(entry.getVk().toString());
                
                String op = entry.getOpcode().toUpperCase();
                debug("Operands: vj=" + vj + ", vk=" + vk);
                
                if (op.contains("ADD")) {
                    double result = vj + vk;
                    debug("ADD result: " + result);
                    return result;
                } else if (op.contains("SUB")) {
                    double result = vj - vk;
                    debug("SUB result: " + result);
                    return result;
                }
            } catch (NumberFormatException e) {
                debug("Number format error: " + e.getMessage());
            }
        } else {
            debug("One or both operands are null: vj=" + entry.getVj() + ", vk=" + entry.getVk());
        }
        
        debug("Default result = 0.0");
        return 0.0;
    }
    
    private void markInstructionExecStart(String tag, int cycle) {
        debug("markInstructionExecStart: tag=" + tag + " cycle=" + cycle);
        for (int i = 0; i < instructionStatuses.size(); i++) {
            InstructionStatus status = instructionStatuses.get(i);
            if (status.tag != null && status.tag.equals(tag) && status.execStartCycle == -1) {
                status.execStartCycle = cycle;
                debug("Found instruction at index " + i + ", set execStartCycle=" + cycle);
                break;
            }
        }
    }
    
    private void markInstructionExecEnd(String tag, int cycle) {
        debug("markInstructionExecEnd: tag=" + tag + " cycle=" + cycle);
        for (int i = 0; i < instructionStatuses.size(); i++) {
            InstructionStatus status = instructionStatuses.get(i);
            if (status.tag != null && status.tag.equals(tag) && status.execEndCycle == -1) {
                status.execEndCycle = cycle;
                debug("Found instruction at index " + i + ", set execEndCycle=" + cycle);
                break;
            }
        }
    }
    
    private void markInstructionWriteBack(String tag, int cycle) {
        debug("markInstructionWriteBack: tag=" + tag + " cycle=" + cycle);
        debug("Looking through " + instructionStatuses.size() + " instruction statuses");
        boolean found = false;
        for (int i = 0; i < instructionStatuses.size(); i++) {
            InstructionStatus status = instructionStatuses.get(i);
            if (status.tag != null && status.tag.equals(tag) && status.writeBackCycle == -1) {
                status.writeBackCycle = cycle;
                debug("FOUND! Set writeBackCycle=" + cycle + " for instruction at index " + i);
                found = true;
                break;
            }
        }
        if (!found) {
            debug("WARNING: Could not find instruction with tag " + tag + " for write-back!");
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
        if (cache != null) cache.clear();
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
        this.blockSize = blockSz;
        this.cacheHitLatency = hitLat;
        this.cacheMissPenalty = missPen;

        if (program != null) {
            initializeSimulator();
        }
    }

    public void setConfigurationWithLatencies(int fpAdd, int fpMul, int intAlu,
                                              int loadBufSize, int storeBufSize,
                                              int cacheSz, int blockSz, int hitLat, int missPen,
                                              int fpAddLat, int fpMulLat, int fpDivLat, int intLat,
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
        this.fpDivLatency = fpDivLat;
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
 