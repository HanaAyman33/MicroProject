package guc.edu.sim. core;

import java.util. HashMap;
import java.util. Map;

/**
 * Register file supporting both integer and floating-point registers.
 * Stores values and tracks which reservation station is producing a pending result.
 */
public class RegisterFile {
    private final Map<String, Double> values = new HashMap<>();
    private final Map<String, String> producerTags = new HashMap<>(); // Qi field

    public RegisterFile() {
        // Initialize integer registers R0-R31
        for (int i = 0; i < 32; i++) {
            values.put("R" + i, 0.0);
            producerTags.put("R" + i, null);
        }
        // Initialize FP registers F0-F31
        for (int i = 0; i < 32; i++) {
            values.put("F" + i, 0.0);
            producerTags.put("F" + i, null);
        }
    }

    public void setValue(String reg, double value) {
        if (reg != null && values.containsKey(reg)) {
            values.put(reg, value);
            System.out.println("[RegFile] " + reg + " = " + value);
        }
    }

    public double getValue(String reg) {
        return values.getOrDefault(reg, 0.0);
    }

    public void setProducer(String reg, String tag) {
        if (reg != null) {
            producerTags. put(reg, tag);
        }
    }

    public String getProducer(String reg) {
        return producerTags.get(reg);
    }

    public boolean isReady(String reg) {
        return producerTags.get(reg) == null;
    }

    public void clearProducer(String reg) {
        producerTags.put(reg, null);
    }

    public Map<String, Double> getAllValues() {
        return new HashMap<>(values);
    }

    public Map<String, String> getAllProducers() {
        return new HashMap<>(producerTags);
    }

    public void loadInitialValues(Map<String, Double> initialValues) {
        initialValues.forEach((reg, val) -> {
            if (values.containsKey(reg)) {
                values.put(reg, val);
            }
        });
    }
}