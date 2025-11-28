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
import java.util.List;

import guc.edu.sim.core.Cache;
import guc.edu.sim.core.Instruction;
import guc.edu.sim.core.InstructionType;
import guc.edu.sim.core.LoadStoreBuffer;
import guc.edu.sim.core.Program;
import guc.edu.sim.core.ProgramLoader;
import guc.edu.sim.core.RegisterFile;
import guc.edu.sim.core.ReservationStationEntry;
import guc.edu.sim.core.SimulatorState;
import guc.edu.sim.core.StationType;
import guc.edu.sim.core.SimulationClock;

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
    @FXML private TableView<LoadStoreView> loadStoreTable;

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

    @FXML private TableColumn<LoadStoreView, String> colLsName;
    @FXML private TableColumn<LoadStoreView, String> colLsOp;
    @FXML private TableColumn<LoadStoreView, String> colLsAddress;
    @FXML private TableColumn<LoadStoreView, Boolean> colLsBusy;

    @FXML private Label addSubBusyLabel;
    @FXML private Label mulDivBusyLabel;
    @FXML private Label loadStoreBusyLabel;

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
    @FXML private TextField loadStoreSizeField;

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

    // Data
    private final ObservableList<InstructionRowView> instructions = FXCollections.observableArrayList();
    private final ObservableList<ReservationStationView> addSubStations = FXCollections.observableArrayList();
    private final ObservableList<ReservationStationView> mulDivStations = FXCollections.observableArrayList();
    private final ObservableList<LoadStoreView> loadStoreBuffers = FXCollections.observableArrayList();
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
        setupRegisterTables();
        setupCacheTable();

        // Start with empty UI; program will be loaded from file
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

        // Load/Store
        colLsName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLsOp.setCellValueFactory(new PropertyValueFactory<>("op"));
        colLsAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colLsBusy.setCellValueFactory(new PropertyValueFactory<>("busy"));
        loadStoreTable.setItems(loadStoreBuffers);
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
        log("▶ Starting continuous simulation.. .");
        
        // Run simulation in background thread
        new Thread(() -> {
            while (isRunning && sim. getIssueUnit().hasNext()) {
                try {
                    Thread.sleep(500); // 500ms delay between cycles
                    Platform.runLater(() -> {
                        if (isRunning) {
                            stepSimulation();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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

    @FXML
    private void onStep() {
        if (sim == null || !sim.isProgramLoaded()) {
            log("⚠ No program loaded.  Use File > Open Program.. .");
            return;
        }
        stepSimulation();
    }

    private void stepSimulation() {
        try {
            boolean issued = sim.step();
            cycle = sim.getCycle();
            
            // Update UI with backend state
            updateReservationStations();
            updateLoadStoreBuffers();
            updateRegisters();
            updateCacheView();
            
            refreshAllLabels();
            
            if (issued) {
                int idx = sim.getLastIssuedIndex();
                if (idx >= 0 && idx < instructions.size()) {
                    instructions.get(idx).issueProperty().set(String.valueOf(cycle));
                }
            }
            
            log("⏭ Stepped to cycle " + cycle);
            updateStatusBar("Executed cycle " + cycle);
            
        } catch (Exception e) {
            log("❌ Error during simulation step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateReservationStations() {
        addSubStations. clear();
        mulDivStations.clear();
        
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
                }
            }
        }
    }

    private void updateLoadStoreBuffers() {
        loadStoreBuffers.clear();
        
        if (sim. getLoadStoreBuffer() != null) {
            for (LoadStoreBuffer. LoadStoreEntry entry : sim.getLoadStoreBuffer().getBuffer()) {
                String address = entry.baseReady ? 
                    String.valueOf(entry.computeAddress()) : 
                    "Waiting";
                
                String destSrc = (entry.instruction.getType() == InstructionType.LOAD) ? 
                    entry.instruction.getDest() : 
                    entry. instruction.getSrc1();
                
                LoadStoreView view = new LoadStoreView(
                    entry.tag,
                    entry.instruction.getOpcode(),
                    address,
                    destSrc != null ? destSrc : "-",
                    true
                );
                loadStoreBuffers.add(view);
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
                double value = rf. getValue(reg);
                String valueStr = String.format("%.1f", value);
                String producer = rf.getProducer(reg);
                String producerStr = (producer != null) ? producer : "";
                
                intRegisters.add(new RegisterView(reg, valueStr, producerStr));
            }
            
            // FP registers
            for (int i = 0; i < 32; i++) {
                String reg = "F" + i;
                double value = rf. getValue(reg);
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
            cacheMisses = cache. getMisses();
            
            Cache.CacheLine[] lines = cache.getLines();
            for (int i = 0; i < lines.length; i++) {
                Cache.CacheLine line = lines[i];
                String tagStr = line.isValid() ? String.valueOf(line.getTag()) : "-";
                String dataStr = line.isValid() ? bytesToHex(line.getData()) : "-";
                
                cacheLines. add(new CacheLineView(i, line.isValid(), tagStr, dataStr));
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            sb.append(String. format("%02X ", bytes[i]));
        }
        return sb.toString(). trim();
    }

    private void updateInstructionTable() {
        // Update execution status in instruction table
        // This would require tracking execution state per instruction
        // You can enhance this based on your needs
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
        loadStoreBuffers.clear();
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
        log("📄 Creating new program...");
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
                renderProgram(sim.getProgram());
                cycle = SimulationClock.getCycle();
                refreshAllLabels();
                log("📂 Loaded program: " + file.getName());
                updateStatusBar("Loaded: " + file.getName());
            } catch (Exception ex) {
                log("❌ Failed to load program: " + ex.getMessage());
                updateStatusBar("Load failed");
            }
        }
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

    // Removed test case loaders; program loads from file now.

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
        log("✓ Configuration applied");
        updateStatusBar("Configuration updated");
        refreshConfigLabels();
        // TODO: Apply configuration to simulation engine
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
        loadStoreSizeField.setText("3");
        
        cacheSizeField.setText("64");
        blockSizeField.setText("16");
        cacheHitLatencyField.setText("1");
        cacheMissPenaltyField.setText("10");
        
        registerInitArea.clear();
        
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
        alert.setContentText("1. Load a program using File > Open or load a test case\n" +
                "2. Configure latencies and station sizes in the Configuration tab\n" +
                "3. Use Run, Pause, or Step buttons to execute\n" +
                "4. Monitor execution in the Program Execution tab\n" +
                "5. View reservation stations, registers, and cache in respective tabs\n" +
                "6. Check Statistics tab for performance metrics");
        alert.showAndWait();
    }

    @FXML
    private void onExportStats() {
        log("📊 Exporting statistics...");
        // TODO: Export to CSV or text file
    }

    // ========== Helper Methods ==========

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
                    .filter(i -> !i.getWriteBack().equals("-"))
                    .count();
            instrCountLabel.setText(completed + " / " + instructions.size());
        }
    }

    private void refreshRsLabels() {
        if (addSubBusyLabel != null) {
            long busy = addSubStations.stream().filter(ReservationStationView::isBusy).count();
            addSubBusyLabel.setText(busy + " / " + addSubStations.size() + " Busy");
        }
        if (mulDivBusyLabel != null) {
            long busy = mulDivStations.stream().filter(ReservationStationView::isBusy).count();
            mulDivBusyLabel.setText(busy + " / " + mulDivStations.size() + " Busy");
        }
        if (loadStoreBusyLabel != null) {
            long busy = loadStoreBuffers.stream().filter(LoadStoreView::isBusy).count();
            loadStoreBusyLabel.setText(busy + " / " + loadStoreBuffers.size() + " Busy");
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
            hitRateLabel.setText(String.format("%.1f%%", rate));
        }
    }

    private void refreshStatistics() {
        if (totalCyclesLabel != null) totalCyclesLabel.setText(String.valueOf(cycle));
        
        if (completedInstrLabel != null) {
            long completed = instructions.stream()
                    .filter(i -> !i.getWriteBack().equals("-"))
                    .count();
            completedInstrLabel.setText(String.valueOf(completed));
        }
        
        if (ipcLabel != null && cycle > 0) {
            long completed = instructions.stream()
                    .filter(i -> !i.getWriteBack().equals("-"))
                    .count();
            double ipc = (double) completed / cycle;
            ipcLabel.setText(String.format("%.2f", ipc));
        }
        
        if (cpiLabel != null && cycle > 0) {
            long completed = instructions.stream()
                    .filter(i -> !i.getWriteBack().equals("-"))
                    .count();
            if (completed > 0) {
                double cpi = (double) cycle / completed;
                cpiLabel.setText(String.format("%.2f", cpi));
            }
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
