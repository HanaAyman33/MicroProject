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
    private Map<String, Double> initialRegValues = new HashMap<>();
    private Map<Integer, Double> initialMemValues = new HashMap<>();

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
                markInstructionWriteBack(pr.tag, currentCycle);
            }
        }
        
        // Phase 1: Write-back - broadcast results from completed execution units
        List<ReservationStationEntry> finishedRS = dispatcher.tickUnits();
        for (ReservationStationEntry entry : finishedRS) {
            System.out.println("[WriteBack] " + entry. getId() + " completed execution");
            double result = (entry.getResult() instanceof Double) ?
                (Double) entry.getResult() : 0.0;

            // Record exec end at the moment the unit finishes before write-back
            markInstructionExecEnd(entry.getId(), currentCycle);

            // Schedule the broadcast for the next cycle to avoid same-cycle write-back
            pendingResults.add(new PendingResult(entry.getId(), result, true));
            rs.removeEntry(entry);

        }
        
        // Phase 2: Execute LOAD operations
        List<LoadBuffer.LoadEntry> completedLoads = new ArrayList<>();
        for (LoadBuffer.LoadEntry loadEntry : loadBuffer.getBuffer()) {
            if (loadEntry.isReady() && !loadEntry.executing) {
                loadEntry.executing = true;
                int addr = loadEntry.computeAddress();
                
                Cache.CacheAccessResult result = cache.access(addr, memory);
                loadEntry.remainingCycles = result.latency;
                loadEntry.result = loadFromMemory(loadEntry.instruction, addr);
                System.out.println("[LoadBuffer] " + loadEntry.tag + " LOADING from address " + addr + 
                                 " (latency=" + result.latency + " cycles)");
                markInstructionExecStart(loadEntry.tag, currentCycle);
            }
            
            if (loadEntry.executing) {
                loadEntry.remainingCycles--;
                System.out.println("[LoadBuffer] " + loadEntry.tag + " executing...  " + 
                                 loadEntry.remainingCycles + " cycles remaining");
                
                if (loadEntry.remainingCycles <= 0) {
                    System.out.println("[LoadBuffer] " + loadEntry.tag + " COMPLETED with value " + loadEntry.result);
                    markInstructionExecEnd(loadEntry.tag, currentCycle);
                    pendingResults.add(new PendingResult(loadEntry.tag, loadEntry.result, true));
                    completedLoads. add(loadEntry);
                }
            }
        }
        
        // Remove completed load entries
        for (LoadBuffer. LoadEntry entry : completedLoads) {
            loadBuffer.removeEntry(entry);
        }
        
        // Phase 3: Execute STORE operations
        List<StoreBuffer.StoreEntry> completedStores = new ArrayList<>();
        for (StoreBuffer. StoreEntry storeEntry : storeBuffer.getBuffer()) {
            if (storeEntry.isReady() && !storeEntry.executing) {
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
            
            if (storeEntry.executing) {
                storeEntry.remainingCycles--;
                System.out.println("[StoreBuffer] " + storeEntry.tag + " executing... " + 
                                 storeEntry.remainingCycles + " cycles remaining");
                
                if (storeEntry.remainingCycles <= 0) {
                    System.out.println("[StoreBuffer] " + storeEntry. tag + " COMPLETED");
                    markInstructionExecEnd(storeEntry.tag, currentCycle);
                    pendingResults.add(new PendingResult(storeEntry.tag, storeEntry.storeValue, false));
                    completedStores.add(storeEntry);
                }
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
        
        if (issueUnit.hasNext()) {
            Instruction instr = program.get(issueUnit.getPc());
            
            boolean canIssue = false;
            switch (instr.getType()) {
                case ALU_FP:
                case ALU_INT:
                    canIssue = rs.hasFreeFor(instr);
                    if (canIssue) {
                        int sizeBefore = rs.getStations().size();
                        rs.accept(instr, null);
                        // Get the newly added entry's tag
                        if (rs.getStations().size() > sizeBefore) {
                            assignedTag = rs. getStations().get(rs. getStations().size() - 1).getId();
                        }
                        System.out.println("[Issue] Issued to RS: " + instr.getOpcode() + " -> " + assignedTag);
                    }
                    break;
                    
                case LOAD:
                    canIssue = loadBuffer.hasFree();
                    if (canIssue) {
                        int sizeBefore = loadBuffer.getBuffer().size();
                        loadBuffer.accept(instr);
                        // Get the newly added entry's tag
                        if (loadBuffer.getBuffer().size() > sizeBefore) {
                            assignedTag = loadBuffer. getBuffer().get(loadBuffer. getBuffer().size() - 1).tag;
                        }
                        System.out.println("[Issue] Issued to Load Buffer: " + instr.getOpcode() + " -> " + assignedTag);
                    }
                    break;
                    
                case STORE:
                    canIssue = storeBuffer.hasFree();
                    if (canIssue) {
                        int sizeBefore = storeBuffer.getBuffer(). size();
                        storeBuffer.accept(instr);
                        // Get the newly added entry's tag
                        if (storeBuffer.getBuffer().size() > sizeBefore) {
                            assignedTag = storeBuffer. getBuffer().get(storeBuffer.getBuffer().size() - 1).tag;
                        }
                        System.out.println("[Issue] Issued to Store Buffer: " + instr.getOpcode() + " -> " + assignedTag);
                    }
                    break;
                    
                case BRANCH:
                    canIssue = branchUnit.isFree();
                    if (canIssue) {
                        branchUnit.accept(instr, null);
                        assignedTag = "BRANCH";
                        System.out. println("[Issue] Issued to Branch Unit: " + instr.getOpcode());
                    }
                    break;
            }
            
            if (canIssue) {
                instr.setIssueCycle(currentCycle);
                instructionStatuses.get(prevPc).issueCycle = currentCycle;
                instructionStatuses.get(prevPc).tag = assignedTag;  // FIXED: Store the tag
                issueUnit.jumpTo(prevPc + 1);
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
