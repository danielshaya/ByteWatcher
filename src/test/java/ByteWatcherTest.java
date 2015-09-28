/*
 * Copyright (C) 2015 Daniel Shaya and Heinz Max Kabutz
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Heinz Max Kabutz licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by daniel on 23/07/2015.
 */
public class ByteWatcherTest {

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

    ByteWatcher am = new ByteWatcher();
    am.onThreadDied(th -> assertNotSame(th, t));
    System.out.println("--------------------");
    t.interrupt();
    Thread.sleep(1000);
    am.printAllAllocations();
  }

  @Test
  public void testThreadCreated() throws InterruptedException {
    ByteWatcher am = new ByteWatcher();
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
    long limit = 1<<20;
    ByteWatcher am = new ByteWatcher();
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
