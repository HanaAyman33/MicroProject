package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Dispatcher manages reservation station entries and execution units.
 * - Keeps a list of active RS entries (these entries are created by your issue/RS allocation logic)
 * - Periodically dispatches ready RS entries to idle execution units that match the station type
 * - Ticks execution units and returns finished entries for write-back
 *
 * Important: This dispatcher is intentionally lightweight. Integrate it with your existing
 * ReservationStation class/list so the entries here are the same objects used by the rest of the simulator.
 */
public class Dispatcher {
    private final LatencyConfig latencyConfig;
    private final List<ExecutionUnit> units = new ArrayList<>();
    private final List<ReservationStationEntry> entries = new ArrayList<>();

    public Dispatcher(LatencyConfig latencyConfig) {
        this.latencyConfig = latencyConfig;
    }

    public void addExecutionUnit(StationType type) {
        units.add(new ExecutionUnit(type, latencyConfig));
    }

    public void addExecutionUnit(StationType type, int count) {
        for (int i = 0; i < count; i++) addExecutionUnit(type);
    }

    public void addEntry(ReservationStationEntry entry) {
        entries.add(entry);
    }

    /**
     * Dispatch ready entries to idle units (one per unit per cycle).
     * Simple greedy strategy: iterate entries, for each ready entry find idle unit of same type and start.
     */
    public void dispatch() {
        for (ReservationStationEntry entry : entries) {
            if (!entry.isReady()) continue;
            for (ExecutionUnit u : units) {
                if (u.isIdle() && u.getUnitType() == entry.getType()) {
                    boolean started = u.start(entry);
                    if (started) break; // entry assigned
                }
            }
        }
    }

    /**
     * Advance all execution units by one cycle. Collect finished entries for write-back.
     */
    public List<ReservationStationEntry> tickUnits() {
        List<ReservationStationEntry> finished = new ArrayList<>();
        for (ExecutionUnit u : units) {
            Optional<ReservationStationEntry> res = u.tick();
            res.ifPresent(entry -> {
                finished.add(entry);
                // remove from dispatch entries if present
                Iterator<ReservationStationEntry> it = entries.iterator();
                while (it.hasNext()) {
                    if (it.next().getId().equals(entry.getId())) {
                        it.remove();
                        break;
                    }
                }
            });
        }
        return finished;
    }

    public List<ReservationStationEntry> getPendingEntries() {
        return new ArrayList<>(entries);
    }
}