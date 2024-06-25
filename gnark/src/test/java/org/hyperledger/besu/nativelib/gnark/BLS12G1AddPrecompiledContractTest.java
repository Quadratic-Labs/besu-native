/*
 * Copyright Besu Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.hyperledger.besu.nativelib.gnark;

import com.google.common.base.Stopwatch;
import com.google.common.io.CharStreams;
import com.sun.jna.ptr.IntByReference;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class BLS12G1AddPrecompiledContractTest {

  @Parameterized.Parameter(0)
  public String input;
  @Parameterized.Parameter(1)
  public String expectedResult;
  @Parameterized.Parameter(2)
  public String expectedGasUsed;
  @Parameterized.Parameter(3)
  public String notes;

  @Parameterized.Parameters
  public static Iterable<String[]> parameters() throws IOException {
    return CharStreams.readLines(
            new InputStreamReader(
                BLS12G1AddPrecompiledContractTest.class.getResourceAsStream("g1_add.csv"), UTF_8))
        .stream()
        .map(line -> line.split(",", 4))
        .collect(Collectors.toList());
  }

  @Test
  public void shouldCalculate() {
    if ("input".equals(input)) {
      // skip the header row
      return;
    }
    final byte[] input = Bytes.fromHexString(this.input).toArrayUnsafe();

    final byte[] output = new byte[LibGnarkEIP2537.EIP2537_PREALLOCATE_FOR_RESULT_BYTES];
    final IntByReference outputLength = new IntByReference();
    final byte[] error = new byte[LibGnarkEIP2537.EIP2537_PREALLOCATE_FOR_ERROR_BYTES];
    final IntByReference errorLength = new IntByReference();

      LibGnarkEIP2537.eip2537_perform_operation(LibGnarkEIP2537.BLS12_G1ADD_OPERATION_SHIM_VALUE,
          input, input.length, output, outputLength, error, errorLength);

    final Bytes expectedComputation =
        expectedResult == null ? null : Bytes.fromHexString(expectedResult);
    if (errorLength.getValue() > 0) {
      assertThat(new String(error, 0, errorLength.getValue(), UTF_8)).isEqualTo(notes);
      assertThat(outputLength.getValue()).isZero();
    } else {
      final Bytes actualComputation = Bytes.wrap(output, 0, outputLength.getValue());
      assertThat(actualComputation).isEqualTo(expectedComputation);
    }
  }
}
