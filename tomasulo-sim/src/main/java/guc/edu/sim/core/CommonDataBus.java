package guc.edu.sim.core;

import java.util.*;

public class CommonDataBus {

    public static final class Broadcast {
        public final String tag;          
        public final String destination;  
        public final Object value;
        private Broadcast(String tag, String destination, Object value) {
            this.tag = tag;
            this.destination = destination;
            this.value = value;
        }
    }

    public interface Subscriber {
        void onBroadcast(String tag, Object value);
    }

    private final List<Subscriber> subscribers = new ArrayList<>();
    private final Deque<Broadcast> queue = new ArrayDeque<>();

    public void subscribe(Subscriber s) {
        if (s != null) subscribers.add(s);
    }

    public void enqueue(String tag, String dest, Object value) {
        if (tag == null) return; 
        queue.addLast(new Broadcast(tag, dest, value));
    }

    public boolean hasPending() { 
        return !queue.isEmpty(); 
    }

    public Broadcast tickAndBroadcast() {
        if (queue.isEmpty()) return null;
        Broadcast b = queue.removeFirst();
        for (Subscriber s : subscribers) {
            s.onBroadcast(b.tag, b.value);
        }
        return b;
    }
}
