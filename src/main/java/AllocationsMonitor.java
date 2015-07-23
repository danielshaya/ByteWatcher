import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by daniel on 22/07/2015.
 * This class allows a user to receive callbacks if
 * threads are destroyed or created.
 * Its primary function is to alert the user if any
 * thread has exceeded a specified amount of allocation.
 */
public class AllocationsMonitor {
    public static final String ALLOCATED = " allocated ";
    private ConcurrentMap<Thread, AllocationMonitorSingleThread> ams;
    private volatile Consumer<Thread> threadDiedConsumer;
    private volatile Consumer<Thread> threadCreatedConsumer;
    private volatile long bytesThreshold;
    private volatile BiConsumer<Thread, Long> allocationExceededConsumer;

    public AllocationsMonitor() {
        ams = Thread.getAllStackTraces()
                .keySet()
                .stream()
                .map(AllocationMonitorSingleThread::new)
                .collect(Collectors.toConcurrentMap(
                        AllocationMonitorSingleThread::getThread,
                        (AllocationMonitorSingleThread am) -> am));
        // Heinz: Makes sense, right? ;-)
        Thread monitorThread = new Thread(new Monitor(), "MonitorThread");
        monitorThread.start();
    }

    public void printAllAllocations() {
        ams.values()
                .stream()
                .forEach(this::printInfo);
    }

    private void printInfo(AllocationMonitorSingleThread am) {
        System.out.println(am.getThread().getName()
                + ALLOCATED + am.calculateAllocations());
    }

    public void onAllocationExceeded(long bytesThreshold,
                                     BiConsumer<Thread, Long> allocationExceededConsumer) {
        this.bytesThreshold = bytesThreshold;
        this.allocationExceededConsumer = allocationExceededConsumer;
    }

    public void onThreadDied(Consumer<Thread> threadDiedConsumer) {
        this.threadDiedConsumer = threadDiedConsumer;
    }

    public void onThreadCreated(Consumer<Thread> threadCreatedConsumer) {
        this.threadCreatedConsumer = threadCreatedConsumer;
    }

    public void reset(){
        ams.values().forEach(AllocationMonitorSingleThread::reset);
    }

    class Monitor implements Runnable {

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                checkThreads();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void checkThreads() {
            //Check if any threads have died
            ams.keySet()
                    .stream()
                    .filter(t -> t.getState().equals(Thread.State.TERMINATED))
                    .forEach(t -> {
                        ams.remove(t);
                        if (threadDiedConsumer != null)
                            threadDiedConsumer.accept(t);
                    });


            Set<Thread> liveThreads = Thread.getAllStackTraces().keySet();
            //Check if any threads have been created
            liveThreads.stream()
                    .filter(t -> ams.containsKey(t) == false)
                    .map(AllocationMonitorSingleThread::new)
                    .forEach(am -> {
                        ams.put(am.getThread(), am);
                        if (threadCreatedConsumer != null)
                            threadCreatedConsumer.accept(am.getThread());
                    });

            if (allocationExceededConsumer != null) {
                ams.values().stream()
                        .filter(am -> am.calculateAllocations() > bytesThreshold)
                        .forEach(am -> allocationExceededConsumer.accept(am.getThread(),
                                am.calculateAllocations()));
            }
        }
    }
}
