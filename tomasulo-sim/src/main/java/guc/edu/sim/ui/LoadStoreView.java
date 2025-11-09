package guc.edu.sim.ui;

import javafx.beans.property.*;

public class LoadStoreView {

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty op = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();

    public LoadStoreView(String name, String op, String address, boolean busy) {
        this.name.set(name);
        this.op.set(op);
        this.address.set(address);
        this.busy.set(busy);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getOp() { return op.get(); }
    public StringProperty opProperty() { return op; }

    public String getAddress() { return address.get(); }
    public StringProperty addressProperty() { return address; }

    public boolean isBusy() { return busy.get(); }
    public BooleanProperty busyProperty() { return busy; }
}
