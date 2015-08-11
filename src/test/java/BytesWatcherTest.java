import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by daniel on 23/07/2015.
 */
public class BytesWatcherTest {

  @Test
  public void testThreadRemoved() throws InterruptedException {
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

    BytesWatcher am = new BytesWatcher();
    am.onThreadDied(th -> assertNotSame(th, t));
    System.out.println("--------------------");
    t.interrupt();
    Thread.sleep(1000);
    am.printAllAllocations();
  }

  @Test
  public void testThreadCreated() throws InterruptedException {
    BytesWatcher am = new BytesWatcher();
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
    Thread.sleep(1000);

    am.printAllAllocations();
  }

  @Test
  public void testAllocationExceeded() throws InterruptedException {
    long limit = 1_000_000;
    BytesWatcher am = new BytesWatcher();
    am.onByteWatch((t, size) ->
        System.out.printf("%s exceeded limit: %d using: %d%n",
            t.getName(), limit, size)
        , limit);
    am.printAllAllocations();
    System.out.println("-------------------------------------------------------------");

    Thread t = new Thread("Allocating Thread") {
      public void run() {
        try {
          List<String> strings = new ArrayList<>();
          int counter = 0;
          while (true) {
            Thread.sleep(200);
            for (int i = 0; i < 100000; i++) {
              strings.add("Add counter:" + counter);
              counter++;
            }
          }
        } catch (InterruptedException consumeAndExit) {
          System.out.println("Thread exit");
        }
      }
    };
    t.start();

    Thread.sleep(10000);
  }
}
