package guc.edu.sim.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegisterFile {
    private final Map<String, Object> regs = new HashMap<>();

    public RegisterFile() {
        for (int i = 0; i < 32; i++) {
            regs.put("R" + i, 0L);
            regs.put("F" + i, 0.0);
        }
    }

    public Object read(String name) {
        return regs.get(name);
    }

    public void write(String name, Object value) {
        if (name == null) return;
        regs.put(name, value);
    }

    public int getRegisterValue(String register) {
        Object value = regs.get(register);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0; // Default value if register not found or not a number
    }
    
    public void setRegisterValue(String register, int value) {
        if (register != null) {
            regs.put(register, value);
        }
    }
    
    public void preload(Map<String, Object> initialValues) {
        if (initialValues == null) return;
        for (Map.Entry<String, Object> e : initialValues.entrySet()) {
            String k = e.getKey();
            if (regs.containsKey(k)) regs.put(k, e.getValue());
        }
    }

    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(regs));
    }
}
