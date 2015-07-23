import javax.management.*;
import java.lang.management.*;
import java.util.concurrent.atomic.*;

/**
 * A class to measure how much allocation there has been on
 * an individual thread.  The class would be useful to embed
 * into regression tests to make sure that
 * there has been no unintended allocation.
 */
public class AllocationMonitorSingleThread {
    private static final String GET_THREAD_ALLOCATED_BYTES =
            "getThreadAllocatedBytes";
    private static final String[] SIGNATURE =
            new String[]{long.class.getName()};
    private static final MBeanServer mBeanServer;
    private static final ObjectName name;

    private final String threadName;
    private final Thread thread;

    private final Object[] PARAMS;
    private final AtomicLong allocated = new AtomicLong();
    private final long BYTES_USED_TO_MEASURE; // usually about 336
    private final long tid;
    private final boolean checkThreadSafety;

    static {
        try {
            name = new ObjectName(
                    ManagementFactory.THREAD_MXBEAN_NAME);
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (MalformedObjectNameException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public AllocationMonitorSingleThread() {
        this(Thread.currentThread(), true);
    }

    public AllocationMonitorSingleThread(Thread thread) {
        this(thread, false);
    }

    private AllocationMonitorSingleThread(Thread thread,
                                          boolean checkThreadSafety) {
        this.checkThreadSafety = checkThreadSafety;
        this.tid = thread.getId();
        this.thread = thread;
        threadName = thread.getName();
        PARAMS = new Object[]{tid};

        // calibrate
        for (int i = 0; i < 1000; i++) {
            // run a few loops to allow for startup anomalies
            calculateAllocations();
        }
        long calibrate = threadAllocatedBytes();
        BYTES_USED_TO_MEASURE = threadAllocatedBytes() - calibrate;
        System.out.println("BYTES_USED_TO_MEASURE:" + BYTES_USED_TO_MEASURE);
        reset();
    }

    public void reset() {
        checkThreadSafety();

        allocated.set(threadAllocatedBytes());
    }

    long threadAllocatedBytes() {
        try {
            return (long) mBeanServer.invoke(
                    name,
                    GET_THREAD_ALLOCATED_BYTES,
                    PARAMS,
                    SIGNATURE
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Calculates the number of bytes allocated since the last
     * reset().
     */
    public long calculateAllocations() {
        checkThreadSafety();
        long mark1 = ((threadAllocatedBytes() -
                BYTES_USED_TO_MEASURE) - allocated.get());
        return mark1;
    }

    private void checkThreadSafety() {
        if (checkThreadSafety &&
                tid != Thread.currentThread().getId())
            throw new IllegalStateException(
                    "AllocationMeasure must not be " +
                            "used over more than 1 thread.");
    }

    public Thread getThread() {
        return thread;
    }

}
