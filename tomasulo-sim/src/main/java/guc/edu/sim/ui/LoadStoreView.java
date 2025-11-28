package guc. edu.sim.ui;

import javafx.beans.property.*;

public class LoadStoreView {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty op = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty destSrc = new SimpleStringProperty();  // FIXED: Must be destSrc
    private final BooleanProperty busy = new SimpleBooleanProperty();

    // 4-parameter constructor (the one we're using)
    public LoadStoreView(String name, String op, String address, String destSrc, boolean busy) {
        this. name.set(name);
        this.op.set(op);
        this.address.set(address);
        this.destSrc.set(destSrc);
        this.busy.set(busy);
    }

    // 3-parameter constructor (for compatibility)
    public LoadStoreView(String name, String op, String address, boolean busy) {
        this(name, op, address, "-", busy);
    }

    // Name property
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String value) { name.set(value); }

    // Op property
    public String getOp() { return op.get(); }
    public StringProperty opProperty() { return op; }
    public void setOp(String value) { op. set(value); }

    // Address property
    public String getAddress() { return address.get(); }
    public StringProperty addressProperty() { return address; }
    public void setAddress(String value) { address.set(value); }
    
    // DestSrc property - FIXED: This is what was missing! 
    public String getDestSrc() { return destSrc.get(); }
    public StringProperty destSrcProperty() { return destSrc; }
    public void setDestSrc(String value) { destSrc.set(value); }

    // Busy property
    public boolean isBusy() { return busy.get(); }
    public BooleanProperty busyProperty() { return busy; }
    public void setBusy(boolean value) { busy.set(value); }
}