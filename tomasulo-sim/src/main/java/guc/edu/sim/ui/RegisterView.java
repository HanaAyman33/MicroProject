package guc.edu.sim.ui;

import javafx.beans.property.*;

public class RegisterView {

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();
    private final StringProperty tag = new SimpleStringProperty();

    public RegisterView(String name, String value, String tag) {
        this.name.set(name);
        this.value.set(value);
        this.tag.set(tag);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getValue() { return value.get(); }
    public StringProperty valueProperty() { return value; }

    public String getTag() { return tag.get(); }
    public StringProperty tagProperty() { return tag; }
}
