/*
 * Copyright 2007 Google Inc.
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
final class DoubleUtil {

  static long doubleToLongBits(final double v) {
    if (Double.isNaN(v)) {
      // NaN -> 0 11111111 10000000000000000000000
      // IEEE754, NaN exponent bits all 1s, and mantissa is non-zero
      return 0x0FFFL << 51;
    }

    // value < 0 -> 1 ???????? ???????????????????????
    // value > 0 -> 0 ???????? ???????????????????????
    long sign = (v < 0 ? 0x1L << 63 : 0);
    long exponent = 0;

    double absV = Math.abs(v);
    // Infinity -> ? 11111111 00000000000000000000000
    // IEEE754 infinite numbers, exponent all 1s, mantissa is 0
    if (Double.isInfinite(v)) {
      exponent = 0x07FFL << 52;
    } else {
      if (absV == 0.0) {
        // 0.0 -> ? 00000000 00000000000000000000000
        // IEEE754, exponent is 0, mantissa is zero
        // we don't handle negative zero at the moment, it is treated as
        // positive zero
        exponent = 0L;
      } else {
        // get an approximation to the exponent
        // let d = 1.M * 2^E
        // log2(d) = log2(1.M * 2^E)
        // log2(d) = log2(1.M) + log2(2^E)
        // log2(d) = log2(1.M) + E
        // floor(log2(d)) = floor(log(1.M) + E)
        // floor(log2(d)) = E (because log(1.M) always < 1)
        // E = floor(log2(d))
        // E = floor(log(d)/log(2))
        int guess = (int) Math.floor(Math.log(absV) / Math.log(2));
        // force it to -1023, 1023 interval (<= -1023 = denorm/zero)
        guess = Math.max(-1023, Math.min(guess, 1023));

        // Recall that d = 1.M * 2^E, so dividing by 2^E should leave
        // us with 1.M
        // divide away exponent guess
        double exp = Math.pow(2, guess);
        absV = absV / exp;

        // while the number is still bigger than a normalized number
        // increment exponent guess
        // This might occur if there is some precision loss in determining
        // the exponent
        while (absV > 2.0) {
          guess++;
          absV /= 2.0;
        }
        // if the number is smaller than a normalized number
        // decrement exponent. If the exponent becomes zero, and we
        // fail to achieve a normalized mantissa, then this number
        // must be a denormalized value
        while (absV < 1 && guess > 1024) {
          guess--;
          absV *= 2;
        }
        exponent = (guess + 1023l) << 52;
      }
    }
    // if denormalized
    if (exponent <= 0) {
      // denormalized numbers have an exponent of zero, but pretend
      // they have an exponent of 1, so since there is an implicit
      // * 2^1 for denorms, we correct by dividing by 2
      absV /= 2;
    }

    // the input value has now been stripped of its exponent
    // and is in the range [0,2), we strip off the leading decimal
    // and use the remainer as a percentage of the significand value (2^52)
    long mantissa = (long) ((absV % 1) * Math.pow(2, 52));
    return sign | exponent | (mantissa & 0xfffffffffffffL);
  }

  private DoubleUtil() {
  }
}
