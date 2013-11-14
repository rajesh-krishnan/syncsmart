package com.cosocket.syncsmart.cpisync;
import java.util.Set;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;
import java.util.TreeSet;
import com.cosocket.syncsmart.cpisync.PF;
import com.cosocket.syncsmart.cpisync.Solver;
import com.cosocket.syncsmart.cpisync.Polynomial;
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
 * Implements the CPIsync algorithm described in: 
 * Y. Minsky, A. Trachtenberg, R. Zippel, "Set reconciliation with nearly 
 * optimal communication complexity," IEEE Transactions on Information 
 * Theory, Volume 49, Issue 9, September 2003
 * 
 * The protocol details are implemented in com.cosocket.syncsmart.cpisyncproto package
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
public class Reconciler {
    private static final int    svthresh  = 128;   // summary vector threshold, vector must fit in a datagram
    private static final int    maxdiff   = 64;    // adjust for best performance, do not make user-settable
    private static final int    redundant = 4;
    private static final long   datamax   = PF.floorpow2(PF.order() - maxdiff - redundant);
    private static final long[] smp       = sampinit();   
    
    /**
     * Return the maximum value of a key for a data item in the set to be reconciled.
     * The keys are drawn from a finite field of prime order.
     * @return the maximum value of a key for a data item in the set to be reconciled
     */
    public static final long    getDatamax()        {return datamax;}
    /**
     * Computes the hash of the input byte array in the finite field of prime order
     * used by the CPI sync reconciliation algorithm.  The hashing uses basically the
     * the same algorithm used in java.lang.String.hashCode() but extends it
     * to 64 bits, takes the absolute value, computes modulo the order of the field 
     * and further modulo getDatamax() 
     * @param b the byte array for which the hash is to be como
     * @return the computed hash
     */
    public static final long    hash(byte[] b)      {return PF.hash(b, datamax);}
    /**
     * Checks whether the provided input is a valid key, i.e., it is a member of the
     * finite field of prime order used by the CPI sync algorithm and is smaller than
     * getDatamax()
     * @param l the key whose validity is to be checked
     * @return the validity of the key
     */
    public static final boolean validKey(long l)    {return (l >= 0 && l < datamax);}
    /**
     * Generate a valid random key for a possible data item in sets that can be reconciled
     * @param r a random number generator instance to use for generating the result
     * @return the generated random key
     */
    public static final long    randomKey(Random r) {return PF.random(r) % datamax;}
 
    private static final long[] sampinit() { 
        long[] samp = new long[maxdiff + redundant];
        for(int i = 0; i < samp.length; i++) samp[i] = PF.nth(datamax + i);
        return samp;
    }
   
    /**
     * Convenience method to generate a long array from a Set
     * @param s the Set<Long> instance to be converted
     * @return the resulting array containing the same elements as the Set
     */
    public static final long[] toArray(Set<Long> s) {
        int i = 0;
        long[] r = new long[s.size()];
        for(Long j : s) r[i++] = j.longValue();
        return r;
    }
    
    private static final Set<Long> toSet(long[] l) {
        Set<Long> r = new TreeSet<Long>();
        for(int i = 0; i < l.length; i++) r.add(l[i]);
        return r;
    }

    private static final long[] evaluate(long[] data) {
        long[] evl = new long[smp.length];
        for (int i = 0; i < smp.length; i++) {
            evl[i] = 1L;
            for (long j : data) evl[i] = PF.mul(evl[i], PF.sub(smp[i],j));
        }
        return evl;
    }
       
    /**
     * Method to determine whether to use summary vector or CPI sync.
     * Summary vector is used as an optimization when the number of elements in 
     * the set partition is smaller than a pre-configured internal threshold.
     * @param datalen the number of elements in the set partition that fall in [begin, end)
     * @param begin the beginning (inclusive) of the set partition to be reconciled
     * @param end the end (exclusive) of the set partition to be reconciled
     * @return true if summary vector is to be used, false otherwise indicating CPIsync should be used
     */
    public static final boolean isSV(int datalen, long begin, long end) {
        return (end - begin <= svthresh || datalen <= svthresh);
    }

    /**
     * Function to return either the summary vector, or poynomial evaluations for the summary vector
     * at pre-determined sample points.
     * @param sv flags whether summary vectors or CPI synv vectors are to be returned
     * @param data summary vector of the set (or partition)  
     * @return array containing either the summary vector provided or the sycn vector
     */
    public static final long[] syncArray(boolean sv, long[] data) {return sv ? data : evaluate(data);}
    
    /**
     * Function that invokes the actual reconciliation algorithm. Computes set differences if 
     * summary vectors are provided and performs poynomial interpolation and factoring
     * if characteristic polynomial evaluations are provided.
     * @param sv flags whether summary vectors or CPI synv vectors are provided
     * @param dA sync or summary vector from the set or partition to be reconciled from one peer (A) 
     * @param dB sync or summary vector from the set or partition to be reconciled from another peer (B)
     * @param mA size of the set (or partition) at the first peer (A)
     * @param mB size of the set (or partition) at the other peer (B)
     * @param deltas vector is populated with two long[] containing the keys for the computed set differences for A - B and B - A 
     * @return whether the set differences where computed successfully, failure can occur if the differences exceed a threshold 
     * @throws Exception if any exceptions occur during the algorithmic computation 
     */
    public static boolean reconcile(boolean sv, long[] dA, long[] dB, int mA, int mB, Vector<long[]> deltas) throws Exception {
        return sv ? svreconcile(dA, dB, deltas) : cpreconcile(dA, dB, mA, mB, deltas);
    }  
    
    private static final long[] svdiff(long[] svA, long[] svB) {
        Set<Long> s = toSet(svA);
        s.removeAll(toSet(svB));
        return toArray(s);        
    }
    
    private static final boolean svreconcile(long[] svA, long[] svB, Vector<long[]> deltas) {
        if (svA.length == 0 && svB.length == 0) {
            deltas.add(new long[0]); 
            deltas.add(new long[0]); 
            return true;
        }
        long[] diffA = svdiff(svA,svB);
        if(diffA.length > svthresh) return false;   // limit size of push vector
        long[] diffB = svdiff(svB,svA);
        if(diffB.length > svthresh) return false;   // limit size of pull vector 
        deltas.add(diffA);
        deltas.add(diffB);
        return true;
    }

    private static final boolean validate(long[] eA, long[] eB, long[] dltaA, long[] dltaB) {        
        long[] tA = new long[redundant];
        long[] tB = new long[redundant];
        for (int i = 0; i < redundant; i++) {
            int k = maxdiff + i;
            tA[i] = eA[k];
            tB[i] = eB[k];
            for (long j : dltaB) tA[i] = PF.mul(tA[i], PF.sub(smp[k], j));            
            for (long j : dltaA) tB[i] = PF.mul(tB[i], PF.sub(smp[k], j));
            if (tA[i] != tB[i]) return false;
        }
        return true;
    }
    
    private static final boolean cpreconcile(long[] eA, long[] eB, int mA, int mB, Vector<long[]> deltas) throws Exception {
        if (mA < 0 || mB < 0)   return false;
        if (mA - mB > maxdiff)  return false;
        if (mB - mA > maxdiff)  return false;
        if (mA == 0 && mB == 0) {
            deltas.add(new long[0]); 
            deltas.add(new long[0]); 
            return true;
        }        
        if ((mA == mB) && Arrays.equals(eA, eB)) {
            deltas.add(new long[0]); 
            deltas.add(new long[0]); 
            return true;
        }        
        long[] pA = new long[eA.length+1];
        long[] pB = new long[eB.length+1];
        if(Solver.solve(smp, eA, eB, mA, mB, pA, pB)) {
            Polynomial pAP = new Polynomial(pA);
            Polynomial pBP = new Polynomial(pB);
            Polynomial g = pAP.gcd(pBP);
            deltas.add(pAP.div(g)[0].roots());
            deltas.add(pBP.div(g)[0].roots());
            if(validate(eA, eB, deltas.get(0),deltas.get(1))) return true;
        } 
        return false;
    }    
}
