package com.cosocket.syncsmart.cpisync;
import com.cosocket.syncsmart.cpisync.PF;
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
 * Binary partitioning and DFS traversal of integer interval without a tree data structure
 * The entire interval must start at 0 (inclusive) and end at a power of two (exclusive)
 *
 * Full traversal is accomplished using or example:
 *     long[] x = Partition.root(0,PF.floorpow2((1L<<3)+1));
 *     while(!Partition.isDone(x)) Partition.next(Partition.isLeaf(x), x, x);
 *     
 * Instead of isLeaf() another user condition to trigger exploring further depth or backtracking. 
 * An asynchronous protocol may accomplish a distributed traversal using:
 *     if (!Partition.isDone(x)) Partition.next(<myCondition>(x), x, x);   
 * 
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
public class Partition {
    private static final int     beg = 0;
    private static final int     end = 1;
    private static final int     min = 2;
    private static final int     max = 3;
    public  static final long    begin      (long[] x)            {return x[beg];}
    public  static final long    end        (long[] x)            {return x[end];}
    public  static final boolean isDone     (long[] x)            {return x[beg]==0 && x[end]==0;}
    public  static final boolean isLeaf     (long[] x)            {return (x[end]-x[beg]) == 1;}
    private static final boolean isRoot     (long[] x)            {return x[beg] == x[min] && x[end] == x[max];};
    private static final boolean isLeft     (long[] x)            {return (((x[beg]-x[min]) / (x[end]-x[beg])) % 2) == 0;}    
    private static final void    done       (long[] x)            {x[beg]=0; x[end]=0;}
    private static final void    copy       (long[] x, long[] y)  {y[beg]=x[beg];y[end]=x[end];y[min]=x[min];y[max]=x[max];}
    private static final void    leftChild  (long[] x, long[] lc) {copy(x,lc); lc[end] = (x[beg] + x[end])/2;}    
    private static final void    rightChild (long[] x, long[] rc) {copy(x,rc); rc[beg] = (x[beg] + x[end])/2;}
    
    //public  static final long    minimum    (long[] x)            {return x[min];}
    //public  static final long    maximum    (long[] x)            {return x[max];}
    //public  static final long[]  clone      (long[] x)            {long[] r = new long[4]; copy(x,r); return r;}

    private static final boolean isValid(long[] x) {
        return (x[min] == 0 && PF.ispow2(x[max]) && x[max] > 1
                && x[beg] >= x[min] && x[end] <= x[max] 
                && x[end] >  x[beg] && x[end] - x[beg] >= 1);
    }
 
    private static final void parent(long[] x, long[] p) {
        copy(x,p);
        if(isRoot(x)) return;
        if(isLeft(x)) {p[end] = x[end]+(x[end]-x[beg]);} else {p[beg] = x[beg]-(x[end]-x[beg]);};
    }
    
    public static final void next(boolean goUp, long[] x, long[] n) {
        if(goUp == false) {
            if(!isLeaf(x)) {leftChild(x,n);} else next(true,x,n);
        } else {
            if(isRoot(x)) done(n);
            else {
                long[] p = {0,0,0,0};
                parent(x,p);
                if(isLeft(x)) {rightChild(p,n);} else next(true,p,n);
            }
        }
    }

    public static final long[] root(long mn, long mx) throws Exception {
        long[] r = new long[]{mn,mx,mn,mx};
        if(!isValid(r)) throw new Exception("Unacceptable interval");
        return r;
    }
    
    public static final long[] partition(long beg, long end, long mn, long mx) throws Exception {
        long[] r = new long[]{beg,end,mn,mx};
        if(!isValid(r)) throw new Exception("Unacceptable interval");
        return r;
    }
    
    /**
     * The main method tests DFS traversal using methods provided by Partition on the interval [0,16) 
     * @param args ignored
     * @throws Exception e.g., when Partition.root is called with invalid arguments, i.e., not power of two
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Testing DFS traversal of [0,16)");
        long[] x = Partition.root(0,PF.floorpow2((1L<<3)+1));
        while(!Partition.isDone(x)) {
            System.out.print("[" + Partition.begin(x) + "," + Partition.end(x) + ") ");
            Partition.next(Partition.isLeaf(x), x, x);
        }
        System.out.println();
    }
}
