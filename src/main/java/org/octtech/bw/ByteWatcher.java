package org.octtech.bw;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by daniel on 22/07/2015.
 * This class allows a user to receive callbacks if
 * threads are destroyed or created.
 * Its primary function is to alert the user if any
 * thread has exceeded a specified amount of allocation.
 */
public class ByteWatcher {
  public static final int SAMPLING_INTERVAL =
      Integer.getInteger("samplingIntervalMillis", 500);
  public static final Consumer<Thread> EMPTY = a -> { };
  public static final BiConsumer<Thread, Long> BI_EMPTY =
      (a, b) -> { };
  private final Map<Thread, ByteWatcherSingleThread> ams;
  private volatile Consumer<Thread> threadCreated = EMPTY;
  private volatile Consumer<Thread> threadDied =
      EMPTY;
  private volatile ByteWatch byteWatch = new ByteWatch(
      BI_EMPTY, Long.MAX_VALUE
  );

  private static class ByteWatch
      implements BiConsumer<Thread, Long>, Predicate<Long>{
    private final long threshold;
    private final BiConsumer<Thread, Long> byteWatch;

    public ByteWatch(BiConsumer<Thread, Long> byteWatch,
                     long threshold) {
      this.byteWatch = byteWatch;
      this.threshold = threshold;
    }

    public void accept(Thread thread, Long currentBytes) {
      byteWatch.accept(thread, currentBytes);
    }

    public boolean test(Long currentBytes) {
      return threshold < currentBytes;
    }
  }

  private final ScheduledExecutorService monitorService =
      Executors.newSingleThreadScheduledExecutor();

  public ByteWatcher() {
    // do this first so that the worker thread is not considered
    // a "newly created" thread
    monitorService.scheduleAtFixedRate(
        this::checkThreads,
        SAMPLING_INTERVAL, SAMPLING_INTERVAL,
        TimeUnit.MILLISECONDS);

    ams = Thread.getAllStackTraces()
        .keySet()
        .stream()
        .map(ByteWatcherSingleThread::new)
        .collect(Collectors.toConcurrentMap(
            ByteWatcherSingleThread::getThread,
            (ByteWatcherSingleThread am) -> am));
    // Heinz: Streams make sense, right? ;-)
  }

  public void onThreadCreated(Consumer<Thread> action) {
    threadCreated = action;
  }

  public void onThreadDied(Consumer<Thread> action) {
    threadDied = action;
  }

  public void onByteWatch(
      BiConsumer<Thread, Long> action, long threshold) {
    this.byteWatch = new ByteWatch(action, threshold);
  }

  public void shutdown() {
    monitorService.shutdown();
  }

  public void forEach(Consumer<ByteWatcherSingleThread> c) {
    ams.values().forEach(c);
  }

  public void printAllAllocations() {
    forEach(System.out::println);
  }

  public void reset() {
    forEach(ByteWatcherSingleThread::reset);
  }

  private void checkThreads() {
    Set<Thread> oldThreads = ams.keySet();
    Set<Thread> newThreads = Thread.getAllStackTraces().keySet();

    Set<Thread> diedThreads = new HashSet<>(oldThreads);
    diedThreads.removeAll(newThreads);

    Set<Thread> createdThreads = new HashSet<>(newThreads);
    createdThreads.removeAll(oldThreads);

    diedThreads.forEach(this::threadDied);
    createdThreads.forEach(this::threadCreated);
    ams.values().forEach(this::bytesWatch);
  }

  private void threadCreated(Thread t) {
    ams.put(t, new ByteWatcherSingleThread(t));
    threadCreated.accept(t);
  }

  private void threadDied(Thread t) {
    threadDied.accept(t);
  }

  private void bytesWatch(ByteWatcherSingleThread am) {
    ByteWatch bw = byteWatch;
    long bytesAllocated = am.calculateAllocations();
    if (bw.test(bytesAllocated)) {
      bw.accept(am.getThread(), bytesAllocated);
    }
  }
}