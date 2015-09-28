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

public class ByteWatcherSingleThreadTest {
  @Test
  public void testQuietMeasuring() {
    ByteWatcherSingleThread am = new ByteWatcherSingleThread();
    System.out.println("MeasuringCostInBytes = " + am.getMeasuringCostInBytes());
    am.calculateAllocations();
    am.reset();

    // this must not run for too long, otherwise we will pick up
    // anomalies from the HotSpot compilation of this method
    for (int i = 0; i < 10000; i++) {
      long mark1 = am.calculateAllocations();
      assertEquals(0, mark1);
      am.reset();
    }
  }

  @Test
  public void testQuietMeasuringThreadAllocatedBytes() {
    ByteWatcherSingleThread am = new ByteWatcherSingleThread();
    System.out.println("MeasuringCostInBytes = " + am.getMeasuringCostInBytes());
    long[] marks = new long[100_000];
    for (int i = 0; i < 100_000; i++) {
      marks[i] = am.threadAllocatedBytes();
    }

    long prevDiff = -1;
    for (int i = 1; i < 100_000; i++) {
      long diff = marks[i] - marks[i - 1];
      if (prevDiff != diff)
        System.out.println("Allocation changed at iteration " + i + "->" + diff);
      prevDiff = diff;
    }
  }

  @Test
  public void testAllocation() {
    ByteWatcherSingleThread am = new ByteWatcherSingleThread();
    System.out.println("MeasuringCostInBytes = " + am.getMeasuringCostInBytes());

    List<String> strings = new ArrayList<>();
    long prevValue = 0;
    for (int i = 0; i < 1000; i++) {
      strings.add("Add counter:" + i);
      if (i % 100 == 0) {
        long currentValue = am.calculateAllocations();
        assertTrue(am.calculateAllocations() > prevValue);
        prevValue = currentValue;
      }
    }
  }

  //This proves that that the method counts all allocations even
  //if they are subsequently garbage collected.
  @Test
  public void testAllocationAndClean() {
    ByteWatcherSingleThread am = new ByteWatcherSingleThread();
    System.out.println("MeasuringCostInBytes = " + am.getMeasuringCostInBytes());

    List<String> strings = new ArrayList<>();
    long prevValue = 0;
    for (int i = 0; i < 10000; i++) {
      strings.add("Add counter:" + i);
      if (i % 100 == 0) {
        strings = new ArrayList<>();
        long currentValue = am.calculateAllocations();
        assertTrue(am.calculateAllocations() > prevValue);
        prevValue = currentValue;
        System.gc();
      }
    }
  }
}