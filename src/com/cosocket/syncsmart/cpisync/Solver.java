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
 * Implements methods used by the CPI set reconciliation algorithm
 * that are called by the Reconciler class.
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
final class Solver {
    protected static final boolean solve (long[] smp, long[] evA, long[] evB, int mA, int mB, long[] pA, long[] pB) throws Exception {
        int mbar = smp.length;
        int delta = mA - mB;
        int tmA = (mbar + delta) / 2;
        int tmB = (mbar - delta) / 2;
        if(tmA < 0 || tmB < 0 || tmA + tmB <= 0) return false;
        
        long[] evl = new long[mbar];
        for (int i = 0; i < mbar; i++) evl[i] = PF.div(evA[i], evB[i]);

        int rank = tsolv(smp, evl, tmA, tmB, pA, pB);
        if (rank > tmA + tmB)  return false;
        if (rank == tmA + tmB) return true;       
        if (rank == 0) return false;

        int mDiff = tmA + tmB - rank;
        tmA -= mDiff;
        tmB -= mDiff;

        if (tmA <= 0 || tmB <= 0) {
            tmA = rank;
            tmB = 0;
        }
                               
        rank = tsolv(smp, evl, tmA, tmB, pA, pB);
        return (rank == tmA + tmB);
    }

    private static final int tsolv (long[] smp, long[] evl, int mA, int mB, long[] pA, long[] pB) throws Exception {
        long[][] vand = vandermonde(smp, evl, mA, mB);
        int rank      = gauss(vand);        
        if (rank == mA + mB) coeffs(vand, mA, mB, pA, pB);
        return rank;
    }
    
    private static final long[][] vandermonde (long[] smp, long[] evl, int mA, int mB) {
        int nrows = mA + mB;
        int ncols = nrows + 1;
        long[] pv = new long[(mA>mB?mA:mB) + 1];
        long[][] result = new long[nrows][ncols];

        pv[0] = 1;
        for (int i = 0; i < nrows; i++) {
            for (int j = 1; j < pv.length; j++) pv[j] = PF.mul(pv[j-1],smp[i]);
            for (int j = 0; j < mA; j++) result[i][j] = pv[j];
            for (int j = 0; j < mB; j++) 
                result[i][mA + j] = PF.mul(PF.neg(evl[i]), pv[j]);
            result[i][mA + mB] = PF.sub(PF.mul(evl[i], pv[mB]), pv[mA]);
        }
        return result;
    }

    private static final void coeffs (long[][] mat, int mA, int mB, long[] pA, long[] pB) throws Exception {
        long[] c = new long [mA + mB]; 
        // store the solution to the linear system in c
        int i, j;
        for (i = mA + mB - 1; i >= 0; i--) {
            c[i] = PF.div(mat[i][mA + mB], mat[i][i]);
            for (j = 0; j < i; j++) // subtract out the coefficient from the previous entries
                mat[j][mA + mB] = PF.sub(mat[j][mA + mB], PF.mul(c[i], mat[j][i]));
        }
        
        for (i = 0; i < mA; i++) pA[i] = c[i];
        pA[mA] = 1L;
        for (i = mA + 1; i < pA.length; i++) pA[i] = 0;
        
        for (i = 0; i < mB; i++) pB[i] = c[mA + i];
        pB[mB] = 1L;
        for (i = mB + 1; i < pB.length; i++) pB[i] = 0;
    }
        
    // Reduce matrix to row echelon form using Gauss-Jordan elimination
    private static final int gauss(long[][] mat) throws Exception {
        int nrows = mat.length;
        int ncols = mat[0].length;
            
        int npivs = 0;
        for (int j = 0; j < ncols; j++) {
            int pivrow = npivs;
            while (pivrow < nrows && mat[pivrow][j] == 0) pivrow++;
            if (pivrow == nrows) continue;
            
            long[] tmp  = mat[npivs];  // swap the pivot row
            mat[npivs]  = mat[pivrow];
            mat[pivrow] = tmp;           
            pivrow      = npivs;
            
            npivs++;
                
            long factor = PF.inv(mat[pivrow][j]);
            for (int k = 0; k < ncols; k++)  // scale the pivot row
                mat[pivrow][k] = PF.mul(mat[pivrow][k], factor);
                
            for (int i = pivrow + 1; i < nrows; i++)
                addrows(mat, pivrow, i, PF.neg(mat[i][j]));
        }
            
        for (int i = nrows - 1; i >= 0; i--) {
            int pivcol = 0;
            while (pivcol < ncols && mat[i][pivcol] == 0) pivcol++;
            if (pivcol == ncols) continue;
                
            for (int j = i - 1; j >= 0; j--)
                 addrows(mat, i, j, PF.neg(mat[j][pivcol]));
        }
        return rank(mat);
    }
    
    private static final void addrows(long[][] mat, int srcrow, int dstrow, long factor) {
        for (int k = 0, ncols = mat[0].length; k < ncols; k++)
            mat[dstrow][k] = PF.add(mat[dstrow][k], PF.mul(mat[srcrow][k], factor));
    }
    
    private static final int rank (long[][] mat) {
        int r = 0;
        for (int i = 0; i < mat.length; i++) if (mat[i][i] != 0) r++;
        return r;
    }
}
