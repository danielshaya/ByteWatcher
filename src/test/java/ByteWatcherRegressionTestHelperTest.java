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

public class ByteWatcherRegressionTestHelperTest {
  private final ByteWatcherRegressionTestHelper helper =
      new ByteWatcherRegressionTestHelper();

  @Test
  public void testNoAllocation() {
    helper.testAllocationNotExceeded(
        () -> { },
        0
    );
    helper.testAllocationNotExceeded(
        this::methodThatDoesNotAllocateAnything,
        0
    );
  }

  public int methodThatDoesNotAllocateAnything() {
    int x = 42;
    for (int i = 0; i < 10000; i++) {
      x += i % 10;
      x /= i % 2 + 1;
    }
    return x;
  }

  @Test(expected = AssertionError.class)
  public void testSomeAllocation() {
    helper.testAllocationNotExceeded(
        () -> {
          byte[] data = new byte[100];
        },
        0
    );
  }

  @Test
  public void testByteArrayAllocation() {
    System.out.println(methodThatDoesNotAllocateAnything());
    helper.testAllocationNotExceeded(
        () -> {
          byte[] data = new byte[100];
        },
        120
    );
  }

}
