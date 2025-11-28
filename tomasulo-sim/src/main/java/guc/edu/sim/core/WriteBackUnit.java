package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Write-back stage controller:
 * - Takes finished RS entries (one or more) and enqueues their results on the CDB.
 * - Each cycle, calls cdb.tickAndBroadcast() to publish at most one result.
 * - On broadcast:
 *      - Wakes up RS operands waiting on that tag (Vj/Vk + clear Qj/Qk).
 *      - Writes to RegisterFile if regStatus still maps that dest to this tag.
 */
public class WriteBackUnit {

    /**
     * Provides a snapshot of all active reservation station entries,
     * so we can propagate CDB results to their operands.
     */
    public interface EntryProvider {
        List<ReservationStationEntry> getAllEntries();
    }

    private final CommonDataBus cdb;
    private final RegisterFile regs;
    private final SimpleRegisterStatusTable regStatus;
    private Supplier<List<ReservationStationEntry>> entryProvider = ArrayList::new;

public WriteBackUnit(CommonDataBus cdb,
                     RegisterFile regs,
                     SimpleRegisterStatusTable regStatus) {
    this.cdb = cdb;
    this.regs = regs;
    this.regStatus = regStatus;

    // SUBSCRIBE TO CDB AUTOMATICALLY
    cdb.subscribe((tag, value) -> onCDB(tag, value));
}

private void onCDB(String tag, Object value) {
    List<ReservationStationEntry> entries = entryProvider.get();
    if (entries == null) return;

    for (ReservationStationEntry e : entries) {
        if (tag.equals(e.getQj())) e.setVj(value);
        if (tag.equals(e.getQk())) e.setVk(value);
    }
}

    public void setEntryProvider(Supplier<List<ReservationStationEntry>> provider) {
        if (provider != null) this.entryProvider = provider;
    }

    /**
     * Called by functional units once they have completed execution.
     * We enqueue their results on the CDB, but actual broadcast happens
     * later in tick().
     */
    public void acceptFinished(List<ReservationStationEntry> finished) {
        if (finished == null) return;
        for (ReservationStationEntry e : finished) {
            String tag = e.getId();          // RS / FU tag, used as Qi
            Object value = e.getResult();
            String dest = e.getDestination(); // architectural reg name, may be null for stores
            cdb.enqueue(tag, dest, value);
        }
    }

    /**
     * One CDB + write-back cycle:
     * - Broadcast at most one result from the CDB.
     * - Propagate it to RS entries (operand wake-up).
     * - If this result is still the current producer for its destination register,
     *   update the RegisterFile and clear the Qi in the RegisterStatusTable.
     */
public CommonDataBus.Broadcast tick() {
    CommonDataBus.Broadcast b = cdb.tickAndBroadcast();
    if (b == null) return null;

    // Handle register writeback
    if (b.destination != null &&
        b.tag.equals(regStatus.getProducerTag(b.destination))) {

        regs.write(b.destination, b.value);
        regStatus.clearIfTagMatches(b.destination, b.tag);
    }

    return b;
}

}
