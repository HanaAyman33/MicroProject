package guc.edu.sim.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WriteBackUnit {

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
    }

    public void setEntryProvider(Supplier<List<ReservationStationEntry>> provider) {
        if (provider != null) this.entryProvider = provider;
    }

    public void acceptFinished(List<ReservationStationEntry> finished) {
        if (finished == null) return;
        for (ReservationStationEntry e : finished) {
            String tag = e.getId();
            Object value = e.getResult();
            String dest = e.getDestination();
            cdb.enqueue(tag, dest, value);
        }
    }

    public CommonDataBus.Broadcast tick() {
        CommonDataBus.Broadcast b = cdb.tickAndBroadcast();
        if (b == null) return null;

        List<ReservationStationEntry> entries = entryProvider.get();
        if (entries != null) {
            for (ReservationStationEntry e : entries) {
                if (b.tag.equals(e.getQj())) e.setVj(b.value);
                if (b.tag.equals(e.getQk())) e.setVk(b.value);
            }
        }

        if (b.destination != null) {
            String current = regStatus.getProducerTag(b.destination);
            if (current != null && current.equals(b.tag)) {
                regs.write(b.destination, b.value);
                regStatus.clearIfTagMatches(b.destination, b.tag);
            }
        }
        return b;
    }
}
