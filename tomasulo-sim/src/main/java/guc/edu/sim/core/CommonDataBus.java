package guc.edu.sim.core;

import java.util.*;

/**
 * Common Data Bus (CDB) for Tomasulo:
 * - Functional units enqueue completed results as <tag, destReg, value>.
 * - At most ONE broadcast per cycle (tickAndBroadcast).
 * - All subscribers (RS, LS/SB, write-back unit) see the broadcast and
 *   decide locally whether they care about that tag.
 */
public class CommonDataBus {

    public static final class Broadcast {
        public final String tag;          // Producer tag (RS / FU id)
        public final String destination;  // Architectural register name, may be null
        public final Object value;        // Result value

        private Broadcast(String tag, String destination, Object value) {
            this.tag = tag;
            this.destination = destination;
            this.value = value;
        }
    }

    /**
     * Any component that needs to listen to CDB results
     * (e.g., reservation stations, register file wrapper, etc.)
     */
    public interface Subscriber {
        void onBroadcast(String tag, Object value);
    }

    private final List<Subscriber> subscribers = new ArrayList<>();
    private final Deque<Broadcast> queue = new ArrayDeque<>();

    public void subscribe(Subscriber s) {
        if (s != null) subscribers.add(s);
    }

    /**
     * Enqueue a result to be broadcast in some future cycle.
     * We *must* have a non-null tag to identify the producer.
     */
    public void enqueue(String tag, String dest, Object value) {
        if (tag == null) return;  // invalid producer; ignore
        queue.addLast(new Broadcast(tag, dest, value));
    }

    public boolean hasPending() {
        return !queue.isEmpty();
    }

    /**
     * One CDB cycle:
     * - Pop one result from the queue (if any).
     * - Broadcast <tag, value> to all subscribers.
     * - Return the broadcast so higher-level write-back logic
     *   can handle reg-file update & RS operand wake-ups.
     */
    public Broadcast tickAndBroadcast() {
        if (queue.isEmpty()) return null;

        Broadcast b = queue.removeFirst();
        for (Subscriber s : subscribers) {
            s.onBroadcast(b.tag, b.value);
        }
        return b;
    }
}
