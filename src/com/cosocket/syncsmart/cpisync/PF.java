package com.cosocket.syncsmart.cpisync;
import java.util.Random;
/*
Copyright (c) 2013, Cosocket LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

* Neither the name of Cosocket LLC nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * Operations on a Prime Finite Field in 64-bits
 * However (1L<<31) - 1 is used as the default order to avoid cost of peasant multiplies
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
final class PF {
    // Order of the prime field, default largest 31-bit prime, long multiply sans overflow
    // Note: Largest 62-bit prime ((1L<<62) - 57) permits long adds sans overflow, but needs peasant multiplies
    private static long p = (1L<<31) - 1; 

    // Order, size, or characteristic of the field
    public static final long order() {return p;}
    
    // nth element of the field
    public static final long nth(long i) {return i % p;}
    
    // random element of the field
    public static final long random(Random r) {return (r.nextLong() & 0x7fffffffffffffffL) % p;}

    // Addition modulo p
    public static final long add(long x, long y) {return (x + y) % p;}
    
    // Additive inverse modulo p
    public static final long neg(long x) {return (p - x) % p;}
    
    // Subtraction modulo p
    public static final long sub(long x, long y) {return (x >= y ? x - y : (x - y + p) % p);}
    
    // Multiplication modulo p using Russian Peasant Algorithm
    public static final long mul(long x, long y) {       
        long moflo = (1L<<31);  // multiply without overflow if operands < moflo
        if (x >= 0 && x < moflo && y >= 0 && y < moflo) return (x * y) % p;
        long res = 0;
        while (x != 0) {
            if ((x & 1) == 1) res = (res + y) % p;
            x >>= 1;
            y = (y << 1) % p;
        }
        return res;
    }
        
    // Multiplicative inverse using Extended Euclidean algorithm
    public static final long inv(long x) throws Exception {
        if (x == 0) throw new Exception ("Divide by zero");
        long f = p;
        long g = x;
        long a = 0;
        long b = 1;
        while (g != 0) {
            long h = f % g;
            long c = a - f / g * b;
            f = g;
            g = h;
            a = b;
            b = c;
        }
        return (a + p) % p;
    }
    
    // Division modulo p using multiplicative inverse
    public static final long div(long x, long y) throws Exception {return mul(x, inv(y));}
    
    // Exponentiation modulo p using repeated squaring
    public static final long pow(long x, long n) throws Exception {
        if(n < 0)  return div (1, pow (x, (0 - n)));
        long r = 1;
        while (n != 0) {
            if ((n & 1L) != 0) r = mul(r, x);
            n >>= 1;
            x = mul(x, x);
        }
        return r;
    }

    // Hash into finite field, further modulo m
    public static final long hash(byte[] b, long m) {
        long h  = 1125899906842597L; // prime
        for (int i = 0; i < b.length; i++) h = 31*h + b[i];
        return (h & 0x7fffffffffffffffL) % (m > 0 && m < p ? m : p);
    }
    
    // Check whether x is a power of two
    public static final boolean ispow2 (long x) {return (x != 0) && ((x & (x-1)) == 0);}
    
    // Nearest power of two equal or lower
    public static final long floorpow2(long x ) {
        if (x == 0)  return 0;
        x |= (x >> 1);
        x |= (x >> 2);
        x |= (x >> 4);
        x |= (x >> 8);
        x |= (x >> 16);
        x |= (x >> 32);
        return x - (x >> 1);
    }
}
