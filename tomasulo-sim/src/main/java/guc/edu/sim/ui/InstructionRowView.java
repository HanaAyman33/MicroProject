package guc.edu.sim.ui;

import javafx.beans.property.*;

public class InstructionRowView {

    private final IntegerProperty index = new SimpleIntegerProperty();
    private final StringProperty pc = new SimpleStringProperty();
    private final StringProperty instruction = new SimpleStringProperty();
    private final StringProperty issue = new SimpleStringProperty();
    private final StringProperty execStart = new SimpleStringProperty();
    private final StringProperty execEnd = new SimpleStringProperty();
    private final StringProperty writeBack = new SimpleStringProperty();
    private final IntegerProperty iteration = new SimpleIntegerProperty();
    private final IntegerProperty programIndex = new SimpleIntegerProperty();

    public InstructionRowView(int index, String pc, String instruction,
                              String issue, String execStart,
                              String execEnd, String writeBack) {
        this(index, pc, instruction, issue, execStart, execEnd, writeBack, 1, index - 1);
    }
    
    public InstructionRowView(int index, String pc, String instruction,
                              String issue, String execStart,
                              String execEnd, String writeBack,
                              int iteration, int programIndex) {
        this.index.set(index);
        this.pc.set(pc);
        this.instruction.set(instruction);
        this.issue.set(issue);
        this.execStart.set(execStart);
        this.execEnd.set(execEnd);
        this.writeBack.set(writeBack);
        this.iteration.set(iteration);
        this.programIndex.set(programIndex);
    }

    public int getIndex() { return index.get(); }
    public IntegerProperty indexProperty() { return index; }

    public String getPc() { return pc.get(); }
    public StringProperty pcProperty() { return pc; }

    public String getInstruction() { return instruction.get(); }
    public StringProperty instructionProperty() { return instruction; }

    public String getIssue() { return issue.get(); }
    public StringProperty issueProperty() { return issue; }

    public String getExecStart() { return execStart.get(); }
    public StringProperty execStartProperty() { return execStart; }

    public String getExecEnd() { return execEnd.get(); }
    public StringProperty execEndProperty() { return execEnd; }

    public String getWriteBack() { return writeBack.get(); }
    public StringProperty writeBackProperty() { return writeBack; }
    
    public int getIteration() { return iteration.get(); }
    public IntegerProperty iterationProperty() { return iteration; }
    
    public int getProgramIndex() { return programIndex.get(); }
    public IntegerProperty programIndexProperty() { return programIndex; }
}
