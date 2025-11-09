package guc.edu.sim.core;

public class SimulationClock {
    private static int cycle = 0;

    public static int getCycle() { return cycle; }

    public static void nextCycle() { cycle++; }

    public static void reset() { cycle = 0; }
}