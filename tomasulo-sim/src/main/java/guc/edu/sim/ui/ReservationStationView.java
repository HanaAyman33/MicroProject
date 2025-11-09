package guc.edu.sim.ui;

import javafx.beans.property.*;

public class ReservationStationView {

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty op = new SimpleStringProperty();
    private final StringProperty vj = new SimpleStringProperty();
    private final StringProperty vk = new SimpleStringProperty();
    private final StringProperty qj = new SimpleStringProperty();
    private final StringProperty qk = new SimpleStringProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();

    public ReservationStationView(String name, String op,
                                  String vj, String vk,
                                  String qj, String qk,
                                  boolean busy) {
        this.name.set(name);
        this.op.set(op);
        this.vj.set(vj);
        this.vk.set(vk);
        this.qj.set(qj);
        this.qk.set(qk);
        this.busy.set(busy);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getOp() { return op.get(); }
    public StringProperty opProperty() { return op; }

    public String getVj() { return vj.get(); }
    public StringProperty vjProperty() { return vj; }

    public String getVk() { return vk.get(); }
    public StringProperty vkProperty() { return vk; }

    public String getQj() { return qj.get(); }
    public StringProperty qjProperty() { return qj; }

    public String getQk() { return qk.get(); }
    public StringProperty qkProperty() { return qk; }

    public boolean isBusy() { return busy.get(); }
    public BooleanProperty busyProperty() { return busy; }
}
