import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 23/07/2015.
 */
public class AllocationsMonitorTest {
    @Test
    public void testThreadRemoved() {

        Thread t = new Thread("To be shutdown") {
            public void run() {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    System.out.println("Thread exit");
                }
            }
        };
        t.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AllocationsMonitor am = new AllocationsMonitor();
        am.onThreadDied(System.out::println);
        am.printAllAllocations();

        System.out.println("--------------------");

        t.interrupt();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        am.printAllAllocations();
    }

    @Test
    public void testThreadCreated() {
        AllocationsMonitor am = new AllocationsMonitor();
        am.onThreadCreated(System.out::println);
        am.printAllAllocations();
        System.out.println("--------------------");

        Thread t = new Thread("Just created") {
            public void run() {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    System.out.println("Thread exit");
                }
            }
        };
        t.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        am.printAllAllocations();
    }

    @Test
    public void testAllocationExceeded() {
        long limit = 1_000_000;
        AllocationsMonitor am = new AllocationsMonitor();
        am.onAllocationExceeded(limit, (t, size) ->
                System.out.println(t.getName() + " exceeded limit:" + limit
                + " using:" + size));
        am.printAllAllocations();
        System.out.println("--------------------");

        Thread t = new Thread("Allocating Thread") {
            public void run() {
                try {
                    List<String> strings = new ArrayList<>();
                    int counter = 0;
                    while(true){
                        Thread.sleep(200);
                        for (int i = 0; i < 100000; i++) {
                            strings.add("Add counter:" + counter);
                            counter++;
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Thread exit");
                }
            }
        };
        t.start();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
