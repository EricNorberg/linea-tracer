/*
 * Copyright Consensys Software Inc.
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
 */

package net.consensys.linea.zktracer.module.trm;

import java.util.List;

import net.consensys.linea.testing.BytecodeCompiler;
import net.consensys.linea.testing.BytecodeRunner;
import net.consensys.linea.zktracer.opcode.OpCode;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class TrmTracerTest {
  private final Bytes32 RANDOM_STRING_FROM_THE_INTERNET =
      Bytes32.fromHexString(
          "0x"
              + "b18cd834"
              + "b6192fcf"
              + "9f51322e"
              + "a31b31be"
              + "bf8fd38b"
              + "b3e8d512"
              + "3273df51"
              + "9650f978");
  private final Bytes32 CLEARING_STRING =
      Bytes32.fromHexString(
          "0x"
              + "00000000"
              + "00000000"
              + "00000000"
              + "ffffffff"
              + "ffffffff"
              + "ffffffff"
              + "ffffffff"
              + "ffffffff");
  private final Bytes32 BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___MAX_VALUE =
      Bytes32.fromHexString(
          "0x"
              + "ffffffff"
              + "ffffffff"
              + "ffffffff"
              + "00000000"
              + "00000000"
              + "00000000"
              + "00000000"
              + "00000000");
  private final Bytes32 BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___ONE =
      Bytes32.fromHexString(
          "0x"
              + "00000000"
              + "00000000"
              + "00000001"
              + "00000000"
              + "00000000"
              + "00000000"
              + "00000000"
              + "00000000");
  private final Bytes32 BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___RANDOM =
      Bytes32.fromHexString(
          "0x"
              + "b18cd834"
              + "b6192fcf"
              + "9f51322e"
              + "00000000"
              + "00000000"
              + "00000000"
              + "00000000"
              + "00000000");

  @Test
  void testNonCallTinyParamLessThan16() {
    for (int tiny = 0; tiny < 16; tiny++) {
      nonCall(Bytes32.leftPad(Bytes.of(tiny)));
    }
  }

  @Test
  void testNonCallTinyParamAround256() {
    for (int tiny = 0; tiny < 32; tiny++) {
      nonCall(Bytes32.leftPad(Bytes.ofUnsignedLong((long) tiny + 248)));
    }
  }

  @Test
  void testNonCallAddressParameterTinyAfterTrimming() {
    for (int tiny = 0; tiny < 16; tiny++) {
      nonCall(
          RANDOM_STRING_FROM_THE_INTERNET
              .and(BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___MAX_VALUE)
              .or(Bytes32.leftPad(Bytes.of(tiny))));
    }
  }

  @Test
  void testNonCallRandomLarge() {
    nonCall(RANDOM_STRING_FROM_THE_INTERNET);
  }

  @Test
  void testSevenArgCall() {
    for (int tiny = 0; tiny < 16; tiny++) {
      sevenArgCall(tiny);
    }
  }

  @Test
  void testSampleDelegateCall() {
    for (long tiny = 0; tiny < 16; tiny++) {
      sampleDelegateCall(tiny);
    }
  }

  void nonCall(Bytes bytes) {
    BytecodeRunner.of(BytecodeCompiler.newProgram().push(bytes).op(OpCode.EXTCODEHASH).compile())
        .run();
  }

  @Test
  void testTrimToUncoverATinyAddressAndQueryItsBalanceCodeHashAndCodeSize() {
    BytecodeCompiler program = BytecodeCompiler.newProgram();

    List<OpCode> opCodeList = List.of(OpCode.BALANCE, OpCode.EXTCODESIZE, OpCode.EXTCODEHASH);

    for (int i = 0; i < 11; i++) {
      program
          .push(BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___MAX_VALUE)
          .push(i)
          .op(OpCode.OR)
          .op(opCodeList.get(i % 3))
          .push(BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___RANDOM)
          .push(i)
          .op(OpCode.OR)
          .op(opCodeList.get((i + 1) % 3))
          .push(BYTE_STRING_OUTSIDE_OF_ADDRESS_RANGE___ONE)
          .push(i)
          .op(OpCode.OR)
          .op(opCodeList.get((i + 2) % 3));
    }

    BytecodeRunner.of(program.compile()).run();
  }

  void sevenArgCall(long rawAddr) {
    BytecodeRunner.of(
            BytecodeCompiler.newProgram()
                .push(Bytes.fromHexString("0xff")) // rds
                .push(Bytes.fromHexString("0x80")) // rdo
                .push(Bytes.fromHexString("0x44")) // cds
                .push(Bytes.fromHexString("0x19")) // cdo
                .push(Bytes.fromHexString("0xffffffffffff")) // value
                .push(Bytes.ofUnsignedLong(rawAddr)) // address
                .push(Bytes.fromHexString("0xffff")) // gas
                .op(OpCode.CALL)
                .compile())
        .run();
  }

  void sampleDelegateCall(long rawAddr) {
    BytecodeRunner.of(
            BytecodeCompiler.newProgram()
                .push(Bytes.fromHexString("0xff")) // rds
                .push(Bytes.fromHexString("0x80")) // rdo
                .push(Bytes.fromHexString("0x44")) // cds
                .push(Bytes.fromHexString("0x19")) // cdo
                .push(Bytes.ofUnsignedLong(rawAddr)) // address
                .push(Bytes.fromHexString("0xffff")) // gas
                .op(OpCode.DELEGATECALL)
                .compile())
        .run();
  }
}
