package guc.edu.sim.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import guc.edu.sim.core.*;

public class MainController {

    // Top bar status labels
    @FXML private Label cycleLabel;
    @FXML private Label instrCountLabel;
    @FXML private Label statusLabel;

    // Program tab
    @FXML private TableView<InstructionRowView> instructionTable;
    @FXML private TableColumn<InstructionRowView, Number> colInstrIndex;
    @FXML private TableColumn<InstructionRowView, String> colInstrPC;
    @FXML private TableColumn<InstructionRowView, String> colInstrText;
    @FXML private TableColumn<InstructionRowView, String> colIssue;
    @FXML private TableColumn<InstructionRowView, String> colExecStart;
    @FXML private TableColumn<InstructionRowView, String> colExecEnd;
    @FXML private TableColumn<InstructionRowView, String> colWriteBack;
    @FXML private TextArea logArea;
    @FXML private VBox logContainer;

    // RS tab
    @FXML private TableView<ReservationStationView> addSubRsTable;
    @FXML private TableView<ReservationStationView> mulDivRsTable;
    @FXML private TableView<ReservationStationView> intRsTable;

    @FXML private TableColumn<ReservationStationView, String> colRsName1;
    @FXML private TableColumn<ReservationStationView, String> colRsOp1;
    @FXML private TableColumn<ReservationStationView, String> colRsVj1;
    @FXML private TableColumn<ReservationStationView, String> colRsVk1;
    @FXML private TableColumn<ReservationStationView, String> colRsQj1;
    @FXML private TableColumn<ReservationStationView, String> colRsQk1;
    @FXML private TableColumn<ReservationStationView, Boolean> colRsBusy1;

    @FXML private TableColumn<ReservationStationView, String> colRsName2;
    @FXML private TableColumn<ReservationStationView, String> colRsOp2;
    @FXML private TableColumn<ReservationStationView, String> colRsVj2;
    @FXML private TableColumn<ReservationStationView, String> colRsVk2;
    @FXML private TableColumn<ReservationStationView, String> colRsQj2;
    @FXML private TableColumn<ReservationStationView, String> colRsQk2;
    @FXML private TableColumn<ReservationStationView, Boolean> colRsBusy2;

    @FXML private TableColumn<ReservationStationView, String> colIntRsName;
    @FXML private TableColumn<ReservationStationView, String> colIntRsOp;
    @FXML private TableColumn<ReservationStationView, String> colIntRsVj;
    @FXML private TableColumn<ReservationStationView, String> colIntRsVk;
    @FXML private TableColumn<ReservationStationView, String> colIntRsQj;
    @FXML private TableColumn<ReservationStationView, String> colIntRsQk;
    @FXML private TableColumn<ReservationStationView, Boolean> colIntRsBusy;

    // CHANGED: Separate tables for Load and Store buffers
    @FXML private TableView<LoadStoreView> loadBufferTable;
    @FXML private TableView<LoadStoreView> storeBufferTable;

    @FXML private TableColumn<LoadStoreView, String> colLoadName;
    @FXML private TableColumn<LoadStoreView, String> colLoadOp;
    @FXML private TableColumn<LoadStoreView, String> colLoadAddress;
    @FXML private TableColumn<LoadStoreView, String> colLoadDest;
    @FXML private TableColumn<LoadStoreView, Boolean> colLoadBusy;

    @FXML private TableColumn<LoadStoreView, String> colStoreName;
    @FXML private TableColumn<LoadStoreView, String> colStoreOp;
    @FXML private TableColumn<LoadStoreView, String> colStoreAddress;
    @FXML private TableColumn<LoadStoreView, String> colStoreSrc;
    @FXML private TableColumn<LoadStoreView, Boolean> colStoreBusy;

    @FXML private Label addSubBusyLabel;
    @FXML private Label mulDivBusyLabel;
    @FXML private Label intBusyLabel;
    @FXML private Label loadBufferBusyLabel;
    @FXML private Label storeBufferBusyLabel;

    // Registers tab
    @FXML private TableView<RegisterView> intRegTable;
    @FXML private TableView<RegisterView> fpRegTable;

    @FXML private TableColumn<RegisterView, String> colIntRegName;
    @FXML private TableColumn<RegisterView, String> colIntRegValue;
    @FXML private TableColumn<RegisterView, String> colIntRegTag;

    @FXML private TableColumn<RegisterView, String> colFpRegName;
    @FXML private TableColumn<RegisterView, String> colFpRegValue;
    @FXML private TableColumn<RegisterView, String> colFpRegTag;

    // Cache tab
    @FXML private TableView<CacheLineView> cacheTable;
    @FXML private TableColumn<CacheLineView, Number> colCacheIndex;
    @FXML private TableColumn<CacheLineView, Boolean> colCacheValid;
    @FXML private TableColumn<CacheLineView, String> colCacheTag;
    @FXML private TableColumn<CacheLineView, String> colCacheData;

    @FXML private Label cacheHitsLabel;
    @FXML private Label cacheMissesLabel;
    @FXML private Label hitRateLabel;
    @FXML private Label blockSizeLabel;
    @FXML private Label cacheSizeLabel;
    @FXML private Label hitLatencyLabel;
    @FXML private Label missPenaltyLabel;

    // Configuration tab
    @FXML private TextField addLatencyField;
    @FXML private TextField mulLatencyField;
    @FXML private TextField divLatencyField;
    @FXML private TextField intAddLatencyField;
    @FXML private TextField loadLatencyField;
    @FXML private TextField storeLatencyField;
    @FXML private TextField branchLatencyField;

    @FXML private TextField addSubSizeField;
    @FXML private TextField mulDivSizeField;
    @FXML private TextField intRsSizeField;
    @FXML private TextField loadBufferSizeField;   // CHANGED
    @FXML private TextField storeBufferSizeField;  // CHANGED

    @FXML private TextField cacheSizeField;
    @FXML private TextField blockSizeField;
    @FXML private TextField cacheHitLatencyField;
    @FXML private TextField cacheMissPenaltyField;

    @FXML private TextArea registerInitArea;
    @FXML private TextArea memoryInitArea;

    // Statistics tab
    @FXML private Label totalCyclesLabel;
    @FXML private Label completedInstrLabel;
    @FXML private Label ipcLabel;
    @FXML private Label cpiLabel;
    @FXML private Label rawHazardsLabel;
    @FXML private Label warHazardsLabel;
    @FXML private Label wawHazardsLabel;
    @FXML private Label structuralHazardsLabel;
    @FXML private Label loadCountLabel;
    @FXML private Label storeCountLabel;
    @FXML private Label aluCountLabel;
    @FXML private Label branchCountLabel;


    // Menu items
    @FXML private CheckMenuItem showLogMenuItem;
    @FXML private CheckMenuItem showStatsMenuItem;

    // Status bar
    @FXML private Label statusBarLabel;

    // Data - CHANGED: Separate lists for load and store buffers
    private final ObservableList<InstructionRowView> instructions = FXCollections.observableArrayList();
    private final ObservableList<ReservationStationView> addSubStations = FXCollections.observableArrayList();
    private final ObservableList<ReservationStationView> mulDivStations = FXCollections.observableArrayList();
    private final ObservableList<ReservationStationView> integerStations = FXCollections.observableArrayList();
    private final ObservableList<LoadStoreView> loadBuffers = FXCollections.observableArrayList();
    private final ObservableList<LoadStoreView> storeBuffers = FXCollections.observableArrayList();
    private final ObservableList<RegisterView> intRegisters = FXCollections.observableArrayList();
    private final ObservableList<RegisterView> fpRegisters = FXCollections.observableArrayList();
    private final ObservableList<CacheLineView> cacheLines = FXCollections.observableArrayList();

    private int cycle = 0;
    private int cacheHits = 0;
    private int cacheMisses = 0;
    private boolean isRunning = false;
    private SimulatorState sim;

    @FXML
    private void initialize() {
        setupInstructionTable();
        setupRsTables();
        setupLoadStoreBufferTables();  // CHANGED
        setupRegisterTables();
        setupCacheTable();

        refreshAllLabels();
        log("✓ Tomasulo Simulator initialized successfully");
        updateStatusBar("Ready to load program");
    }

    // ========== Table Setup ==========

    private void setupInstructionTable() {
        colInstrIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        colInstrPC.setCellValueFactory(new PropertyValueFactory<>("pc"));
        colInstrText.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colIssue.setCellValueFactory(new PropertyValueFactory<>("issue"));
        colExecStart.setCellValueFactory(new PropertyValueFactory<>("execStart"));
        colExecEnd.setCellValueFactory(new PropertyValueFactory<>("execEnd"));
        colWriteBack.setCellValueFactory(new PropertyValueFactory<>("writeBack"));
        instructionTable.setItems(instructions);
    }

    private void setupRsTables() {
        // Add/Sub
        colRsName1.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRsOp1.setCellValueFactory(new PropertyValueFactory<>("op"));
        colRsVj1.setCellValueFactory(new PropertyValueFactory<>("vj"));
        colRsVk1.setCellValueFactory(new PropertyValueFactory<>("vk"));
        colRsQj1.setCellValueFactory(new PropertyValueFactory<>("qj"));
        colRsQk1.setCellValueFactory(new PropertyValueFactory<>("qk"));
        colRsBusy1.setCellValueFactory(new PropertyValueFactory<>("busy"));
        addSubRsTable.setItems(addSubStations);

        // Mul/Div
        colRsName2.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRsOp2.setCellValueFactory(new PropertyValueFactory<>("op"));
        colRsVj2.setCellValueFactory(new PropertyValueFactory<>("vj"));
        colRsVk2.setCellValueFactory(new PropertyValueFactory<>("vk"));
        colRsQj2.setCellValueFactory(new PropertyValueFactory<>("qj"));
        colRsQk2.setCellValueFactory(new PropertyValueFactory<>("qk"));
        colRsBusy2.setCellValueFactory(new PropertyValueFactory<>("busy"));
        mulDivRsTable.setItems(mulDivStations);

        if (intRsTable != null) {
            colIntRsName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colIntRsOp.setCellValueFactory(new PropertyValueFactory<>("op"));
            colIntRsVj.setCellValueFactory(new PropertyValueFactory<>("vj"));
            colIntRsVk.setCellValueFactory(new PropertyValueFactory<>("vk"));
            colIntRsQj.setCellValueFactory(new PropertyValueFactory<>("qj"));
            colIntRsQk.setCellValueFactory(new PropertyValueFactory<>("qk"));
            colIntRsBusy.setCellValueFactory(new PropertyValueFactory<>("busy"));
            intRsTable.setItems(integerStations);
        }
    }

    // CHANGED: Setup separate Load and Store buffer tables
    private void setupLoadStoreBufferTables() {
        // Load Buffer Table
        if (colLoadName != null) colLoadName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colLoadOp != null) colLoadOp.setCellValueFactory(new PropertyValueFactory<>("op"));
        if (colLoadAddress != null) colLoadAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colLoadDest != null) colLoadDest.setCellValueFactory(new PropertyValueFactory<>("destSrc"));
        if (colLoadBusy != null) colLoadBusy.setCellValueFactory(new PropertyValueFactory<>("busy"));
        if (loadBufferTable != null) loadBufferTable.setItems(loadBuffers);
        
        // Store Buffer Table
        if (colStoreName != null) colStoreName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colStoreOp != null) colStoreOp.setCellValueFactory(new PropertyValueFactory<>("op"));
        if (colStoreAddress != null) colStoreAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colStoreSrc != null) colStoreSrc.setCellValueFactory(new PropertyValueFactory<>("destSrc"));
        if (colStoreBusy != null) colStoreBusy.setCellValueFactory(new PropertyValueFactory<>("busy"));
        if (storeBufferTable != null) storeBufferTable.setItems(storeBuffers);
    }

    private void setupRegisterTables() {
        colIntRegName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colIntRegValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colIntRegTag.setCellValueFactory(new PropertyValueFactory<>("tag"));
        intRegTable.setItems(intRegisters);

        colFpRegName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFpRegValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colFpRegTag.setCellValueFactory(new PropertyValueFactory<>("tag"));
        fpRegTable.setItems(fpRegisters);
    }

    private void setupCacheTable() {
        colCacheIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        colCacheValid.setCellValueFactory(new PropertyValueFactory<>("valid"));
        colCacheTag.setCellValueFactory(new PropertyValueFactory<>("tag"));
        colCacheData.setCellValueFactory(new PropertyValueFactory<>("data"));
        cacheTable.setItems(cacheLines);
    }

    // ========== Program Rendering ==========

    private void renderProgram(Program program) {
        instructions.clear();
        if (program == null) return;
        List<Instruction> list = program.getInstructions();
        for (int i = 0; i < list.size(); i++) {
            Instruction instr = list.get(i);
            String pcStr = String.format("0x%04X", i * 4);
            String text = toDisplayString(instr);
            instructions.add(new InstructionRowView(i + 1, pcStr, text, "-", "-", "-", "-"));
        }
        refreshInstructionCount();
    }

    private String toDisplayString(Instruction instr) {
        if (instr == null) return "";
        String op = instr.getOpcode();
        switch (instr.getType()) {
            case LOAD:
                return String.format("%s %s, %d(%s)", op, nullSafe(instr.getDest()),
                        instr.getOffset() == null ? 0 : instr.getOffset(), nullSafe(instr.getBase()));
            case STORE:
                return String.format("%s %s, %d(%s)", op, nullSafe(instr.getSrc1()),
                        instr.getOffset() == null ? 0 : instr.getOffset(), nullSafe(instr.getBase()));
            case BRANCH:
                return String.format("%s %s, %s, %s", op, nullSafe(instr.getSrc1()),
                        nullSafe(instr.getSrc2()), nullSafe(instr.getBranchTargetLabel()));
            case ALU_FP:
            case ALU_INT:
                if (instr.getSrc2() != null) {
                    return String.format("%s %s, %s, %s", op, nullSafe(instr.getDest()),
                            nullSafe(instr.getSrc1()), nullSafe(instr.getSrc2()));
                } else {
                    return String.format("%s %s, %s", op, nullSafe(instr.getDest()),
                            nullSafe(instr.getSrc1()));
                }
            default:
                return op;
        }
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

    // ========== Button Handlers ==========

    @FXML
    private void onRun() {
        if (isRunning) {
            log("⚠ Simulation already running");
            return;
        }
        if (sim == null || ! sim.isProgramLoaded()) {
            log("⚠ No program loaded");
            return;
        }
        
        isRunning = true;
        statusLabel.setText("Running");
        statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        log("▶ Starting continuous simulation...");
        
        // Run simulation in background thread
        new Thread(() -> {
            // Keep running while:
            // 1. There are instructions to issue, OR
            // 2. There are instructions that haven't completed (not all write-backs are done)
            while (isRunning && (sim.getIssueUnit().hasNext() || hasInstructionsInProgress())) {
                try {
                    Thread.sleep(500); // 500ms delay between cycles
                    Platform.runLater(() -> {
                        if (isRunning) {
                            stepSimulation();
                        }
                    });
                    // Wait for UI update to complete before checking again
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread. currentThread().interrupt();
                    break;
                }
            }
            Platform.runLater(() -> {
                isRunning = false;
                statusLabel.setText("Completed");
                statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
                log("✓ Simulation completed");
            });
        }).start();
    }
    
 // Check if there are any instructions still in progress (not yet written back)
    private boolean hasInstructionsInProgress() {
        if (sim == null) {
            return false;
        }
        
        // Check if there are any reservation stations busy
        if (sim.getReservationStations() != null) {
            for (ReservationStationEntry entry : sim.getReservationStations(). getStations()) {
                // If any station is busy, we have instructions in progress
                return true;
            }
        }
        
        // Check if there are any load buffer entries
        if (sim.getLoadBuffer() != null && ! sim.getLoadBuffer().getBuffer().isEmpty()) {
            return true;
        }
        
        // Check if there are any store buffer entries
        if (sim.getStoreBuffer() != null && !sim.getStoreBuffer().getBuffer().isEmpty()) {
            return true;
        }
        
        // Check instruction statuses to see if any haven't written back yet
        if (sim.getInstructionStatuses() != null) {
            List<SimulatorState.InstructionStatus> statuses = sim.getInstructionStatuses();
            for (SimulatorState.InstructionStatus status : statuses) {
                // If instruction was issued but hasn't written back, it's still in progress
                if (status. issueCycle > 0 && status.writeBackCycle <= 0) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @FXML
    private void onPause() {
        if (!isRunning) {
            log("⚠ Simulation not running");
            return;
        }
        isRunning = false;
        statusLabel.setText("Paused");
        statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #FF9800;");
        log("⏸ Simulation paused at cycle " + cycle);
        updateStatusBar("Simulation paused");
    }

    @FXML
    private void onStep() {
        if (sim == null || !sim.isProgramLoaded()) {
            log("⚠ No program loaded.Use File > Open Program.. .");
            return;
        }
        stepSimulation();
    }

    private void stepSimulation() {
        try {
            System.out.println("\n==================== UI STEPPING ====================");
            
            boolean issued = sim.step();
            cycle = sim.getCycle();
            
            // Update ALL UI components after each step
            System.out.println("[UI] Updating Reservation Stations...");
            updateReservationStations();
            
            System.out.println("[UI] Updating Load/Store Buffers...");
            updateLoadStoreBuffers();
            
            System.out.println("[UI] Updating Registers...");
            updateRegisters();
            
            System.out.println("[UI] Updating Cache...");
            updateCacheView();
            
            System.out.println("[UI] Updating Instruction Table...");
            updateInstructionTable();
            
            System.out.println("[UI] Refreshing labels...");
            refreshAllLabels();
            
            if (issued) {
                int idx = sim.getLastIssuedIndex();
                if (idx >= 0 && idx < instructions.size()) {
                    instructions.get(idx).issueProperty().set(String.valueOf(cycle));
                }
            }
            
            log("⏭ Stepped to cycle " + cycle);
            updateStatusBar("Executed cycle " + cycle);
            
            System.out.println("==================== UI UPDATE COMPLETE ====================\n");
            
        } catch (Exception e) {
            log("❌ Error during simulation step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onReset() {
        cycle = 0;
        cacheHits = 0;
        cacheMisses = 0;
        isRunning = false;
        if (sim != null) sim.reset();
        instructions.clear();
        addSubStations.clear();
        mulDivStations.clear();
        integerStations.clear();
        loadBuffers.clear();          // CHANGED
        storeBuffers.clear();         // CHANGED
        intRegisters.clear();
        fpRegisters.clear();
        cacheLines.clear();
        refreshAllLabels();
        statusLabel.setText("Ready");
        statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #666;");
        log("⟲ Simulation reset to initial state");
        updateStatusBar("Simulation reset");
    }

    @FXML
    private void onNewProgram() {
        log("📄 Creating new program.. .");
        updateStatusBar("Ready for new program");
        // TODO: Open program editor dialog
    }

    @FXML
    private void onOpenProgram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Program File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Assembly Files", "*.asm", "*.s"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showOpenDialog(logArea.getScene().getWindow());
        if (file != null) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                if (sim == null) sim = new SimulatorState();
                sim.loadProgramLines(lines);
                
                // Apply configuration with defaults (don't call onApplyConfig which requires UI fields)
                applyDefaultConfiguration();
                
                renderProgram(sim.getProgram());
                cycle = SimulationClock.getCycle();
                
                // Initialize UI with register values
                updateRegisters();
                updateReservationStations();
                updateLoadStoreBuffers();
                updateCacheView();
                
                refreshAllLabels();
                log("📂 Loaded program: " + file.getName());
                updateStatusBar("Loaded: " + file.getName());
            } catch (Exception ex) {
                log("❌ Failed to load program: " + ex.getMessage());
                ex.printStackTrace();
                updateStatusBar("Load failed");
            }
        }
    }

    // New helper method to apply default configuration
    private void applyDefaultConfiguration() {
        if (sim == null || ! sim.isProgramLoaded()) return;
        
        // Get values from UI if available, otherwise use defaults
        int fpAdd = getIntValue(addSubSizeField, 3);
        int fpMul = getIntValue(mulDivSizeField, 2);
        int intRs = getIntValue(intRsSizeField, 2);
        int loadBufSize = getIntValue(loadBufferSizeField, 3);
        int storeBufSize = getIntValue(storeBufferSizeField, 3);
        int cacheSz = getIntValue(cacheSizeField, 64);
        int blockSz = getIntValue(blockSizeField, 16);
        int hitLat = getIntValue(cacheHitLatencyField, 1);
        int missPen = getIntValue(cacheMissPenaltyField, 10);
        int fpAddLat = getIntValue(addLatencyField, 3);
        int fpMulLat = getIntValue(mulLatencyField, 10);
        int fpDivLat = getIntValue(divLatencyField, 40);
        int intLat = getIntValue(intAddLatencyField, 1);
        int loadLat = getIntValue(loadLatencyField, 2);
        int storeLat = getIntValue(storeLatencyField, 2);
        int branchLat = getIntValue(branchLatencyField, 1);
        
        sim.setConfigurationWithLatencies(fpAdd, fpMul, intRs, loadBufSize, storeBufSize, 
                cacheSz, blockSz, hitLat, missPen, 
                fpAddLat, fpMulLat, fpDivLat, intLat, loadLat, storeLat, branchLat);
        setConfigFieldTexts(fpAdd, fpMul, intRs, loadBufSize, storeBufSize,
                cacheSz, blockSz, hitLat, missPen,
                fpAddLat, fpMulLat, fpDivLat, intLat, loadLat, storeLat, branchLat);
        
        // Load initial values from text areas if they exist
        parseAndLoadRegisterValues();
        parseAndLoadMemoryValues();
    }

    @FXML
    private void onSaveProgram() {
        log("💾 Saving program...");
        // TODO: Implement save functionality
    }

    @FXML
    private void onLoadConfig() {
        log("⚙ Loading configuration...");
        // TODO: Load configuration from file
    }

    @FXML
    private void onSaveConfig() {
        log("💾 Saving configuration...");
        // TODO: Save configuration to file
    }

    @FXML
    private void onShowConfig() {
        log("⚙ Opening configuration dialog...");
        // Configuration is now in a tab, just switch to it
    }

    @FXML
    private void onExit() {
        log("👋 Exiting simulator...");
        Platform.exit();
    }

    @FXML
    private void onClearLog() {
        logArea.clear();
        log("Log cleared");
    }

    @FXML
    private void onClearCache() {
        for (CacheLineView line : cacheLines) {
            line.validProperty().set(false);
            line.tagProperty().set("");
            line.dataProperty().set("");
        }
        cacheHits = 0;
        cacheMisses = 0;
        refreshCacheStats();
        log("🗑 Cache cleared");
    }

    @FXML
    private void onApplyConfig() {
        try {
            if (sim != null && sim.isProgramLoaded()) {
                // Use defaults if fields are null or empty
                int fpAdd = getIntValue(addSubSizeField, 3);
                int fpMul = getIntValue(mulDivSizeField, 2);
                int intRs = getIntValue(intRsSizeField, 2);
                int loadBufSize = getIntValue(loadBufferSizeField, 3);
                int storeBufSize = getIntValue(storeBufferSizeField, 3);
                int cacheSz = getIntValue(cacheSizeField, 64);
                int blockSz = getIntValue(blockSizeField, 16);
                int hitLat = getIntValue(cacheHitLatencyField, 1);
                int missPen = getIntValue(cacheMissPenaltyField, 10);
                int fpAddLat = getIntValue(addLatencyField, 3);
                int fpMulLat = getIntValue(mulLatencyField, 10);
                int fpDivLat = getIntValue(divLatencyField, 40);
                int intLat = getIntValue(intAddLatencyField, 1);
                int loadLat = getIntValue(loadLatencyField, 2);
                int storeLat = getIntValue(storeLatencyField, 2);
                int branchLat = getIntValue(branchLatencyField, 1);
                
                sim.setConfigurationWithLatencies(fpAdd, fpMul, intRs, loadBufSize, storeBufSize,
                         cacheSz, blockSz, hitLat, missPen,
                         fpAddLat, fpMulLat, fpDivLat, intLat, loadLat, storeLat, branchLat);
                setConfigFieldTexts(fpAdd, fpMul, intRs, loadBufSize, storeBufSize,
                        cacheSz, blockSz, hitLat, missPen,
                        fpAddLat, fpMulLat, fpDivLat, intLat, loadLat, storeLat, branchLat);
                
                parseAndLoadRegisterValues();
                parseAndLoadMemoryValues();

                // Re-render UI after the simulator is rebuilt
                renderProgram(sim.getProgram());
                cycle = sim.getCycle();
                updateReservationStations();
                updateLoadStoreBuffers();
                updateRegisters();
                updateCacheView();
                refreshAllLabels();
                
                log("✓ Configuration applied successfully");
                updateStatusBar("Configuration updated");
                refreshConfigLabels();
            } else {
                log("⚠ Load a program first before applying configuration");
            }
        } catch (Exception e) {
            log("❌ Error applying configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to safely get integer value from TextField
    private int getIntValue(TextField field, int defaultValue) {
        if (field == null) return defaultValue;
        try {
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @FXML
    private void onResetConfig() {
        // Reset to default values
        addLatencyField.setText("2");
        mulLatencyField.setText("10");
        divLatencyField.setText("40");
        intAddLatencyField.setText("1");
        loadLatencyField.setText("2");
        storeLatencyField.setText("2");
        branchLatencyField.setText("1");
        
        addSubSizeField.setText("3");
        mulDivSizeField.setText("2");
        if (intRsSizeField != null) {
            intRsSizeField.setText("2");
        }
        loadBufferSizeField.setText("3");      // CHANGED
        storeBufferSizeField.setText("3");     // CHANGED
        
        cacheSizeField.setText("64");
        blockSizeField.setText("16");
        cacheHitLatencyField.setText("1");
        cacheMissPenaltyField.setText("10");
        
        registerInitArea.clear();
        memoryInitArea.clear();
        
        log("⟲ Configuration reset to defaults");
        refreshConfigLabels();
    }

    @FXML
    private void onLoadRegisterValues() {
        log("📂 Loading register values from file...");
        // TODO: Implement file loading
    }

    @FXML
    private void onResetRegisters() {
        for (RegisterView reg : intRegisters) {
            reg.valueProperty().set("0");
            reg.tagProperty().set("");
        }
        for (RegisterView reg : fpRegisters) {
            reg.valueProperty().set("0.0");
            reg.tagProperty().set("");
        }
        log("⟲ Registers reset to zero");
    }

    @FXML
    private void onExpandAll() {
        log("Expanding all sections");
    }

    @FXML
    private void onCollapseAll() {
        log("Collapsing all sections");
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Tomasulo Simulator");
        alert.setHeaderText("Tomasulo Algorithm Simulator");
        alert.setContentText("A dynamic scheduling simulator implementing the Tomasulo algorithm.\n\n" +
                "Developed for CSEN 702: Microprocessors\n" +
                "Winter 2025\n\n" +
                "Features:\n" +
                "• Out-of-order execution\n" +
                "• Register renaming\n" +
                "• Hazard detection and resolution\n" +
                "• Cache simulation\n" +
                "• Performance metrics");
        alert.showAndWait();
    }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Guide");
        alert.setHeaderText("How to Use the Simulator");
        alert.setContentText("1.Load a program using File > Open or load a test case\n" +
                "2.Configure latencies and station sizes in the Configuration tab\n" +
                "3.Use Run, Pause, or Step buttons to execute\n" +
                "4.Monitor execution in the Program Execution tab\n" +
                "5.View reservation stations, registers, and cache in respective tabs\n" +
                "6.Check Statistics tab for performance metrics");
        alert.showAndWait();
    }

    @FXML
    private void onExportStats() {
        log("📊 Exporting statistics.. .");
        // TODO: Export to CSV or text file
    }

    // ========== Helper Methods ==========

    private void parseAndLoadRegisterValues() {
        if (registerInitArea == null || sim == null) return;
        
        String text = registerInitArea.getText(). trim();
        if (text.isEmpty()) return;
        
        Map<String, Double> regValues = new HashMap<>();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            // Format: R2=100 or F4=3.5
            String[] parts = line.split("=");
            if (parts.length == 2) {
                String reg = parts[0].trim();
                try {
                    double value = Double.parseDouble(parts[1]. trim());
                    regValues.put(reg, value);
                    log("📝 Setting " + reg + " = " + value);
                } catch (NumberFormatException e) {
                    log("⚠ Invalid value for " + reg + ": " + parts[1]);
                }
            }
        }
        
        sim.loadInitialRegisterValues(regValues);
        updateRegisters(); // Refresh UI
    }

    private void parseAndLoadMemoryValues() {
        if (memoryInitArea == null || sim == null) return;
        
        String text = memoryInitArea.getText().trim();
        if (text.isEmpty()) return;
        
        Map<Integer, Double> memValues = new HashMap<>();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            // Format: 100=45.5 (address=value)
            String[] parts = line.split("=");
            if (parts.length == 2) {
                try {
                    int address = Integer.parseInt(parts[0].trim());
                    double value = Double.parseDouble(parts[1]. trim());
                    memValues.put(address, value);
                    log("📝 Setting Memory[" + address + "] = " + value);
                } catch (NumberFormatException e) {
                    log("⚠ Invalid memory entry: " + line);
                }
            }
        }
        
        sim.loadInitialMemoryValues(memValues);
    }

    private void updateReservationStations() {
        addSubStations.clear();
        mulDivStations.clear();
        integerStations.clear();
        
        if (sim.getReservationStations() != null) {
            for (ReservationStationEntry entry : sim.getReservationStations(). getStations()) {
                String vjStr = (entry.getVj() != null) ? String.valueOf(entry.getVj()) : "-";
                String vkStr = (entry.getVk() != null) ? String.valueOf(entry.getVk()) : "-";
                String qjStr = (entry.getQj() != null) ? entry.getQj() : "-";
                String qkStr = (entry.getQk() != null) ? entry.getQk() : "-";
                
                ReservationStationView view = new ReservationStationView(
                    entry.getId(),
                    entry.getOpcode(),
                    vjStr,
                    vkStr,
                    qjStr,
                    qkStr,
                    true
                );
                
                if (entry.getType() == StationType.FP_ADD) {
                    addSubStations.add(view);
                } else if (entry.getType() == StationType.FP_MUL) {
                    mulDivStations.add(view);
                } else if (entry.getType() == StationType.INTEGER) {
                    integerStations.add(view);
                }
            }
        }
    }

    // CHANGED: Update to handle separate Load and Store buffers
    private void updateLoadStoreBuffers() {
        loadBuffers.clear();
        storeBuffers.clear();
        
        // Update Load Buffer
        if (sim.getLoadBuffer() != null) {
            for (LoadBuffer.LoadEntry entry : sim.getLoadBuffer(). getBuffer()) {
                String address = entry.baseReady ? 
                    String.valueOf(entry.computeAddress()) : 
                    "Waiting";
                
                LoadStoreView view = new LoadStoreView(
                    entry.tag,
                    entry.instruction.getOpcode(),
                    address,
                    entry.instruction.getDest() != null ? entry.instruction.getDest() : "-",
                    true
                );
                loadBuffers.add(view);
            }
        }
        
        // Update Store Buffer
        if (sim.getStoreBuffer() != null) {
            for (StoreBuffer.StoreEntry entry : sim.getStoreBuffer(). getBuffer()) {
                String address = entry.baseReady ? 
                    String.valueOf(entry.computeAddress()) : 
                    "Waiting";
                
                LoadStoreView view = new LoadStoreView(
                    entry.tag,
                    entry.instruction.getOpcode(),
                    address,
                    entry.instruction.getSrc1() != null ? entry.instruction.getSrc1() : "-",
                    true
                );
                storeBuffers.add(view);
            }
        }
    }

    private void updateRegisters() {
        intRegisters.clear();
        fpRegisters.clear();
        
        if (sim.getRegFile() != null) {
            RegisterFile rf = sim.getRegFile();
            
            // Integer registers
            for (int i = 0; i < 32; i++) {
                String reg = "R" + i;
                double value = rf.getValue(reg);
                String valueStr = String.format("%.1f", value);
                String producer = rf.getProducer(reg);
                String producerStr = (producer != null) ? producer : "";
                
                intRegisters.add(new RegisterView(reg, valueStr, producerStr));
            }
            
            // FP registers
            for (int i = 0; i < 32; i++) {
                String reg = "F" + i;
                double value = rf.getValue(reg);
                String valueStr = String.format("%.1f", value);
                String producer = rf.getProducer(reg);
                String producerStr = (producer != null) ? producer : "";
                
                fpRegisters.add(new RegisterView(reg, valueStr, producerStr));
            }
        }
    }

    private void updateCacheView() {
        cacheLines.clear();
        
        if (sim.getCache() != null) {
            Cache cache = sim.getCache();
            cacheHits = cache.getHits();
            cacheMisses = cache.getMisses();
            
            Cache.CacheLine[] lines = cache.getLines();
            for (int i = 0; i < lines.length; i++) {
                Cache.CacheLine line = lines[i];
                String tagStr = line.isValid() ? String.valueOf(line.getTag()) : "-";
                String dataStr = line.isValid() ? bytesToHex(line.getData()) : "-";
                
                cacheLines.add(new CacheLineView(i, line.isValid(), tagStr, dataStr));
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString(). trim();
    }

    private void updateInstructionTable() {
        if (sim == null || sim.getInstructionStatuses() == null) return;
        
        List<SimulatorState. InstructionStatus> statuses = sim.getInstructionStatuses();
        
        for (int i = 0; i < Math.min(instructions.size(), statuses.size()); i++) {
            SimulatorState.InstructionStatus status = statuses. get(i);
            InstructionRowView view = instructions.get(i);
            
            if (status.issueCycle > 0) {
                view. issueProperty().set(String. valueOf(status.issueCycle));
            }
            if (status.execStartCycle > 0) {
                view.execStartProperty().set(String.valueOf(status.execStartCycle));
            }
            if (status.execEndCycle > 0) {
                int displayExecEnd = status.execEndCycle - 1;
                view.execEndProperty().set(String.valueOf(displayExecEnd));
            }
            if (status.writeBackCycle > 0) {
                view.writeBackProperty().set(String.valueOf(status.writeBackCycle));
            }
        }
    }

    private void refreshAllLabels() {
        refreshCycleLabel();
        refreshInstructionCount();
        refreshRsLabels();
        refreshCacheStats();
        refreshStatistics();
        refreshConfigLabels();
    }

    private void refreshCycleLabel() {
        if (cycleLabel != null) {
            cycleLabel.setText(Integer.toString(cycle));
        }
    }

    private void refreshInstructionCount() {
        if (instrCountLabel != null) {
            int completed = (int) instructions.stream()
                    .filter(i -> ! i.getWriteBack(). equals("-"))
                    .count();
            instrCountLabel.setText(completed + " / " + instructions.size());
        }
    }

    // CHANGED: Include separate labels for Load and Store buffers
    private void refreshRsLabels() {
        if (sim != null) {
            long busyAdd = addSubStations.stream().filter(ReservationStationView::isBusy).count();
            long busyMul = mulDivStations.stream().filter(ReservationStationView::isBusy).count();
            long busyInt = integerStations.stream().filter(ReservationStationView::isBusy).count();
            long busyLoad = loadBuffers.stream().filter(LoadStoreView::isBusy).count();
            long busyStore = storeBuffers.stream().filter(LoadStoreView::isBusy).count();

            if (addSubBusyLabel != null) {
                addSubBusyLabel.setText(busyAdd + " / " + sim.getFpAddSize() + " Busy");
            }
            if (mulDivBusyLabel != null) {
                mulDivBusyLabel.setText(busyMul + " / " + sim.getFpMulSize() + " Busy");
            }
            if (intBusyLabel != null) {
                intBusyLabel.setText(busyInt + " / " + sim.getIntSize() + " Busy");
            }
            if (loadBufferBusyLabel != null) {
                loadBufferBusyLabel.setText(busyLoad + " / " + sim.getLoadBufferSize() + " Busy");
            }
            if (storeBufferBusyLabel != null) {
                storeBufferBusyLabel.setText(busyStore + " / " + sim.getStoreBufferSize() + " Busy");
            }
        }
    }

    private void refreshCacheStats() {
        if (cacheHitsLabel != null) {
            cacheHitsLabel.setText(String.valueOf(cacheHits));
        }
        if (cacheMissesLabel != null) {
            cacheMissesLabel.setText(String.valueOf(cacheMisses));
        }
        if (hitRateLabel != null) {
            int total = cacheHits + cacheMisses;
            double rate = total > 0 ? (100.0 * cacheHits / total) : 0.0;
            hitRateLabel.setText(String.format("%.1f%%", rate));  // FIXED: %. 1f not %.  1f
        }
    }

    private void refreshStatistics() {
        if (totalCyclesLabel != null) totalCyclesLabel.setText(String.valueOf(cycle));
        
        if (completedInstrLabel != null) {
            long completed = instructions.stream()
                    .filter(i -> ! i.getWriteBack(). equals("-"))
                    .count();
            completedInstrLabel.setText(String.valueOf(completed));
        }
        
        if (ipcLabel != null && cycle > 0) {
            long completed = instructions.stream()
                    . filter(i -> !i.getWriteBack().equals("-"))
                    .count();
            double ipc = (double) completed / cycle;
            ipcLabel.setText(String.format("%.2f", ipc));  // FIXED: %. 2f not %. 2f
        }
        
        if (cpiLabel != null && cycle > 0) {
            long completed = instructions.stream()
                    .filter(i -> !i.getWriteBack().equals("-"))
                    . count();
            if (completed > 0) {
                double cpi = (double) cycle / completed;
                cpiLabel.setText(String.format("%.2f", cpi));  // FIXED: %.2f not %. 2f
            }
        }

        if (sim != null) {
            int raw = sim.getRawHazards();
            setLabelText(rawHazardsLabel, String.valueOf(raw));

            int war = sim.getWarHazards();
            setLabelText(warHazardsLabel, String.valueOf(war));

            int waw = sim.getWawHazards();
            setLabelText(wawHazardsLabel, String.valueOf(waw));

            int structural = sim.getStructuralHazards();
            setLabelText(structuralHazardsLabel, String.valueOf(structural));

            int loads = sim.getLoadIssuedCount();
            setLabelText(loadCountLabel, String.valueOf(loads));

            int stores = sim.getStoreIssuedCount();
            setLabelText(storeCountLabel, String.valueOf(stores));

            int fpOps = sim.getFpIssuedCount();
            int intOps = sim.getIntIssuedCount();
            int totalAlu = fpOps + intOps;
            setLabelText(aluCountLabel, String.valueOf(totalAlu));

            int branches = sim.getBranchIssuedCount();
            setLabelText(branchCountLabel, String.valueOf(branches));
        }
    }

    private void refreshConfigLabels() {
        if (blockSizeLabel != null && blockSizeField != null) {
            blockSizeLabel.setText(blockSizeField.getText() + " bytes");
        }
        if (cacheSizeLabel != null && cacheSizeField != null) {
            cacheSizeLabel.setText(cacheSizeField.getText() + " bytes");
        }
        if (hitLatencyLabel != null && cacheHitLatencyField != null) {
            hitLatencyLabel.setText(cacheHitLatencyField.getText() + " cycle" + 
                    (cacheHitLatencyField.getText().equals("1") ? "" : "s"));
        }
        if (missPenaltyLabel != null && cacheMissPenaltyField != null) {
            missPenaltyLabel.setText(cacheMissPenaltyField.getText() + " cycles");
        }
    }

    private void updateStatusBar(String message) {
        if (statusBarLabel != null) {
            statusBarLabel.setText(message);
        }
    }

    private void setConfigFieldTexts(int fpAdd, int fpMul, int intStations,
                                     int loadBuf, int storeBuf,
                                     int cacheSz, int blockSz, int hitLat, int missPen,
                                     int fpAddLat, int fpMulLat, int fpDivLat, int intLat,
                                     int loadLat, int storeLat, int branchLat) {
        setField(addSubSizeField, fpAdd);
        setField(mulDivSizeField, fpMul);
        setField(intRsSizeField, intStations);
        setField(loadBufferSizeField, loadBuf);
        setField(storeBufferSizeField, storeBuf);
        setField(cacheSizeField, cacheSz);
        setField(blockSizeField, blockSz);
        setField(cacheHitLatencyField, hitLat);
        setField(cacheMissPenaltyField, missPen);
        setField(addLatencyField, fpAddLat);
        setField(mulLatencyField, fpMulLat);
        setField(divLatencyField, fpDivLat);
        setField(intAddLatencyField, intLat);
        setField(loadLatencyField, loadLat);
        setField(storeLatencyField, storeLat);
        setField(branchLatencyField, branchLat);
    }

    private void setField(TextField field, int value) {
        if (field != null) {
            field.setText(Integer.toString(value));
        }
    }

    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void log(String message) {
        if (logArea == null) return;
        Platform.runLater(() -> {
            String timestamp = String.format("[Cycle %d] ", cycle);
            if (logArea.getText().isEmpty()) {
                logArea.setText(timestamp + message);
            } else {
                logArea.appendText("\n" + timestamp + message);
            }
            // Auto-scroll to bottom
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
