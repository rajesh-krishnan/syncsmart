package com.cosocket.syncsmart;
import java.util.Vector;
import com.cosocket.syncsmart.cpisync.Partition;
import com.cosocket.syncsmart.cpisync.Reconciler;
import com.cosocket.syncsmart.cpisyncproto.Store;

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
 * Tests the CPIsync algorithm using two local Store objects. No protocol elements are exercised. 
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
public class LocalTest {
    private static String  s               = "METADATA";
    private static Store<String> storeA = new Store<String>(s.getBytes());
    private static Store<String> storeB = new Store<String>(s.getBytes());

    /**
     * Constructs two Store instances and populates the first set with the number of specified items and the second set such
     * that the specified number differences between the sets are achieved. Then it calls the Reconciler.reconcile method
     * to reconcile the differences. Any computed differences result in transferring the items corresponding to the differences
     * from one set to the other. Upon failure the process is repeated on a partition and the space is traversed in DFS order. 
     * @param args first-set-size, number-of-differences; 10000 and 100 are chosen if no command line arguments are provided.
     * @throws Exception when any of the components throw an exception
     */
    public static void main(String[] args) throws Exception {
        populateSets(args);
        
        System.out.println("Start reconciling");
        long start_time, end_time;
        start_time = System.currentTimeMillis();
                    
        int nmessages = 0;
        Vector<long[]> deltas = new Vector<long[]>();
        long[] n = Partition.root(0,Reconciler.getDatamax());
        while(!Partition.isDone(n)) { 
            nmessages++;
            long    begin = Partition.begin(n);
            long    end   = Partition.end(n);
            long[]  dB    = storeB.keySetArray(begin, end);
            int     mB    = dB.length;
            boolean sv    = Reconciler.isSV(mB, begin, end);
            long[]  eB    = Reconciler.syncArray(sv, dB);

            long[]  dA    = storeA.keySetArray(begin, end);
            int     mA    = dA.length;
            long[]  eA    = Reconciler.syncArray(sv, dA);
            
            //System.out.println(begin + " " + end + " " + mA + " " + mB + " " + (sv ? "SV" : "CPI"));
            boolean success = Reconciler.reconcile(sv, eA, eB, mA, mB, deltas);
            if(success) handleDeltas(deltas.get(0), deltas.get(1));
            Partition.next(success, n, n);
            deltas.clear();
        }
        
        end_time = System.currentTimeMillis();
        System.out.println("Time: " + (end_time-start_time));
        System.out.println("Messages: " + nmessages);
        System.out.println("A has: " + storeA.cardinality() + " B has: " + storeB.cardinality());  
    }
    
    private static void populateSets(String[] args) throws Exception {
         int nsetA, ndiff;
         try {
             nsetA = Integer.parseInt(args[0]);
             ndiff = Integer.parseInt(args[1]);
         } catch (Exception e) {
             nsetA = 10000;
             ndiff = 100;            
         }                 
         int begB = 0;
         int endB = 0;
         nsetA = (nsetA < 0) ? 0 - nsetA : nsetA;
         ndiff = (ndiff < 0) ? 0 - ndiff : ndiff;
         if(ndiff <= nsetA) {
             begB = ndiff / 2;
             endB = nsetA + (ndiff + 1)/2;
         } else {
             begB = nsetA / 2;
             endB = begB + ndiff + (nsetA % 2);
         }  
         System.out.println("Preparing reconcilers A[0:" + (nsetA - 1) + "] and B[" + begB + ":" + (endB - 1) + "]");
         for(long i = 0; i < nsetA; i++) {
             String s = Long.toString(i) + "-item";
             storeA.addIfNew(Reconciler.hash(s.getBytes()), s);
             //storeA.addIfNew(i, s);
         }
         for(long i = begB; i < endB; i++) {
             String s = Long.toString(i) + "-item";
               storeB.addIfNew(Reconciler.hash(s.getBytes()), s);
             //storeB.addIfNew(i, s);
         }
         System.out.println("A has: " + storeA.cardinality() + " B has: " + storeB.cardinality());
    }
    
    private static void handleDeltas(long[] push, long[] pull) throws Exception {     
        if (push.length > 0) {
            for (long i : push) storeB.addIfNew(i, storeA.getValue(i));
            System.out.print("push[" + push.length + "]: ");
            for (long i : push) System.out.print(i + " "); System.out.println();
        }

        if (pull.length > 0) {
            for (long i : pull) storeA.addIfNew(i, storeB.getValue(i));
            System.out.print("pull[" + pull.length + "]: ");
            for (long i : pull) System.out.print(i + " "); System.out.println();
        }
    }
}
