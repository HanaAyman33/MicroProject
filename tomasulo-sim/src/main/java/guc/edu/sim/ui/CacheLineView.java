package guc.edu.sim.ui;

import javafx.beans.property.*;

public class CacheLineView {

    private final IntegerProperty index = new SimpleIntegerProperty();
    private final BooleanProperty valid = new SimpleBooleanProperty();
    private final StringProperty tag = new SimpleStringProperty();
    private final StringProperty data = new SimpleStringProperty();

    public CacheLineView(int index, boolean valid, String tag, String data) {
        this.index.set(index);
        this.valid.set(valid);
        this.tag.set(tag);
        this.data.set(data);
    }

    public int getIndex() { return index.get(); }
    public IntegerProperty indexProperty() { return index; }

    public boolean isValid() { return valid.get(); }
    public BooleanProperty validProperty() { return valid; }

    public String getTag() { return tag.get(); }
    public StringProperty tagProperty() { return tag; }

    public String getData() { return data.get(); }
    public StringProperty dataProperty() { return data; }
}
