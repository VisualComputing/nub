/*
 * Copyright 2010 Google Inc.
 * Copyright 2010 Daniel Bell
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package remixlab.util;

/**
 */
class FloatUtil {

  private static final double LN2 = Math.log(2);

  // Theory of operation: Let a double number d be represented as
  // 1.M * 2^E, where the leading bit is assumed to be 1,
  // the fractional mantissa M is multiplied 2 to the power of E.
  // We want to reliably recover M and E, and then encode them according
  // to IEEE754 (see http://en.wikipedia.org/wiki/IEEE754)
  static int floatToIntBits(float value) {
    if (Float.isNaN(value)) {
      return 0x7fc00000;
    }
    int signBit;
    if (value == 0) {
      return (1 / value == Float.NEGATIVE_INFINITY) ? 0x80000000 : 0;
    } else if (value < 0) {
      value = -value;
      signBit = 0x80000000;
    } else {
      signBit = 0;
    }
    if (value == Float.POSITIVE_INFINITY) {
      return signBit | 0x7f800000;
    }

    int exponent = (int) (Math.log(value) / LN2);
    if (exponent < -126) {
      exponent = -126;
    }
    int significand = (int) (0.5 + value * Math.exp(-(exponent - 23) * LN2));

    // Handle exponent rounding issues & denorm
    if ((significand & 0x01000000) != 0) {
      significand >>= 1;
      exponent++;
    } else if ((significand & 0x00800000) == 0) {
      if (exponent == -126) {
        return signBit | significand;
      } else {
        significand <<= 1;
        exponent--;
      }
    }
    return signBit | ((exponent + 127) << 23) | (significand & 0x007fffff);
  }

  static String toBinaryIeee754String(long decimal) {
    String binary = Long.toBinaryString(decimal);
    StringBuilder result = new StringBuilder(binary);
    for (long i = binary.length(); i < 32; i++) {
      result.insert(0, "0");
    }
    result.insert(9, " ");
    result.insert(1, " ");
    return result.toString();
  }

  private FloatUtil() {
  }
}
