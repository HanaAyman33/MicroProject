package guc. edu.sim.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorState {

    private Program program;
    private IssueUnit issueUnit;
    private RegisterFile regFile;
    private Memory memory;
    private Cache cache;
    private RealReservationStations rs;
    private LoadStoreBuffer loadStoreBuffer;
    private BranchUnit branchUnit;
    private Dispatcher dispatcher;
    private CommonDataBus cdb;
    private SimulationController controller;
    private LatencyConfig latencyConfig;
    
    private int lastIssuedIndex = -1;
    
    // Configuration
    private int fpAddSize = 3;
    private int fpMulSize = 2;
    private int intSize = 2;
    private int loadStoreSize = 3;
    private int cacheSize = 64;
    private int blockSize = 16;
    private int cacheHitLatency = 1;
    private int cacheMissPenalty = 10;

    public void loadProgramLines(List<String> lines) {
        ProgramLoader loader = new ProgramLoader();
        this.program = loader.loadFromLines(lines);
        initializeSimulator();
    }

    private void initializeSimulator() {
        System.out.println("\n========== Initializing Tomasulo Simulator ==========");
        
        // Create components
        latencyConfig = new LatencyConfig();
        regFile = new RegisterFile();
        memory = new Memory();
        cache = new Cache(cacheSize, blockSize, cacheHitLatency, cacheMissPenalty);
        
        rs = new RealReservationStations(fpAddSize, fpMulSize, intSize, regFile);
        loadStoreBuffer = new LoadStoreBuffer(loadStoreSize, regFile, memory, cache);
        branchUnit = new BranchUnit(regFile, program);
        
        dispatcher = new Dispatcher(latencyConfig);
        dispatcher.addExecutionUnit(StationType.FP_ADD, 2);
        dispatcher.addExecutionUnit(StationType.FP_MUL, 1);
        dispatcher.addExecutionUnit(StationType.INTEGER, 1);
        
        cdb = new CommonDataBus();
        cdb.addListener((tag, result) -> {
            rs.broadcastResult(tag, result);
            loadStoreBuffer.broadcastResult(tag, result);
            branchUnit.broadcastResult(tag, result);
            
            // Update register file
            for (String reg : regFile.getAllProducers(). keySet()) {
                if (tag.equals(regFile.getProducer(reg))) {
                    regFile. setValue(reg, result);
                    regFile.clearProducer(reg);
                }
            }
        });
        
        issueUnit = new IssueUnit(program);
        controller = new SimulationController(issueUnit, rs, loadStoreBuffer, branchUnit, null);
        
        SimulationClock. reset();
        this.lastIssuedIndex = -1;
        
        System.out.println("========== Initialization Complete ==========\n");
    }

    public boolean isProgramLoaded() {
        return program != null;
    }

    public boolean step() {
        if (controller == null || issueUnit == null) return false;
        
        System.out.println("\n========== Cycle " + (SimulationClock.getCycle() + 1) + " ==========");
        
        // 1. Write-back phase: broadcast results
        List<ReservationStationEntry> finished = dispatcher.tickUnits();
        for (ReservationStationEntry entry : finished) {
            double result = (entry.getResult() instanceof Double) ? 
                (Double) entry.getResult() : 0.0;
            cdb. broadcast(entry.getId(), result);
            rs.removeEntry(entry);
        }
        
        // Execute load/store operations
        for (LoadStoreBuffer.LoadStoreEntry lsEntry : loadStoreBuffer.getBuffer()) {
            if (lsEntry.isReady() && ! lsEntry.executing) {
                lsEntry.executing = true;
                int addr = lsEntry.computeAddress();
                
                if (lsEntry.instruction.getType() == InstructionType. LOAD) {
                    Cache.CacheAccessResult result = cache.access(addr, memory);
                    lsEntry.remainingCycles = result.latency;
                    lsEntry.result = memory.loadDouble(addr);
                    System.out.println("[LoadStore] " + lsEntry.tag + " loading from addr " + addr);
                } else {
                    lsEntry.remainingCycles = cacheHitLatency;
                    memory.storeDouble(addr, lsEntry.storeValue);
                    System. out.println("[LoadStore] " + lsEntry.tag + " storing to addr " + addr);
                }
            }
            
            if (lsEntry. executing) {
                lsEntry.remainingCycles--;
                if (lsEntry.remainingCycles <= 0) {
                    if (lsEntry.instruction.getType() == InstructionType. LOAD) {
                        cdb.broadcast(lsEntry. tag, lsEntry.result);
                    }
                    loadStoreBuffer.removeEntry(lsEntry);
                }
            }
        }
        
        // Resolve branches
        branchUnit.tryResolve();
        
        // 2.  Execution phase: dispatch ready instructions
        for (ReservationStationEntry entry : rs.getStations()) {
            dispatcher.addEntry(entry);
        }
        dispatcher.dispatch();
        
        // 3. Issue phase
        int prevPc = issueUnit.getPc();
        controller.stepOneCycle();
        boolean issued = issueUnit.getPc() > prevPc;
        lastIssuedIndex = issued ? prevPc : -1;
        
        return issued;
    }

    public void reset() {
        SimulationClock.reset();
        lastIssuedIndex = -1;
        if (issueUnit != null) issueUnit.jumpTo(0);
        if (cache != null) cache.clear();
        initializeSimulator();
    }

    public int getCycle() { return SimulationClock.getCycle(); }
    public Program getProgram() { return program; }
    public int getLastIssuedIndex() { return lastIssuedIndex; }
    public RegisterFile getRegFile() { return regFile; }
    public Cache getCache() { return cache; }
    public RealReservationStations getReservationStations() { return rs; }
    public LoadStoreBuffer getLoadStoreBuffer() { return loadStoreBuffer; }
    
    public void setConfiguration(int fpAdd, int fpMul, int intAlu, int loadStore,
                                 int cacheSz, int blockSz, int hitLat, int missPen) {
        this.fpAddSize = fpAdd;
        this.fpMulSize = fpMul;
        this.intSize = intAlu;
        this.loadStoreSize = loadStore;
        this. cacheSize = cacheSz;
        this.blockSize = blockSz;
        this.cacheHitLatency = hitLat;
        this.cacheMissPenalty = missPen;
    }

	public IssueUnit getIssueUnit() {
		return issueUnit;
	}
}