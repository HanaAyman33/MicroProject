package tomasulo.core;

public interface ReservationStations {
    boolean hasFreeFor(Instruction instr);
    void accept(Instruction instr, RegisterStatusTable regStatus);
}
