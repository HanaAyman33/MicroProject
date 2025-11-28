package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Common Data Bus for broadcasting results to all waiting units.
 */
public class CommonDataBus {
    private final List<BroadcastListener> listeners = new ArrayList<>();

    public void addListener(BroadcastListener listener) {
        listeners.add(listener);
    }

    public void broadcast(String tag, double result) {
        System.out.println("[CDB] Broadcasting " + tag + " = " + result);
        for (BroadcastListener listener : listeners) {
            listener.onBroadcast(tag, result);
        }
    }

    public interface BroadcastListener {
        void onBroadcast(String tag, double result);
    }
}