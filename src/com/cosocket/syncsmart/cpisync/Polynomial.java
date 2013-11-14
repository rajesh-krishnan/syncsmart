package com.cosocket.syncsmart.cpisync;
import java.util.Random;
import java.util.ArrayList;
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
 * Operations on polynomials over a field
 * Adapted from http://www.dustball.com/cs/factor/factorlib-0.1/Polynomial.java
 * We are grateful to the original author Keith Randall who made the code available
 * in the public domain. The code was refactored for the data types used in this
 * package, and an argument that terminates the equal degree factorization after a given
 * degree was introduced. A common use in this package is to factorize into monomials.
 * @author Modifications by Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
final class Polynomial {
    private final long[] coeff;  // (c[0] + c[1]*x + c[2]*x^2 ...), ending non-zero
        
    public Polynomial(long[] coeff) {    
        int len = 0;
        for (int i = coeff.length - 1; i >= 0; i--) { // remove heading zeroes
            if (coeff[i] != 0) {
                len = i + 1;
                break;
            }
        }
        if (len < coeff.length) {
            long[] new_coeff = new long[len];
            System.arraycopy(coeff, 0, new_coeff, 0, len);
            coeff = new_coeff;
        }   
        this.coeff = coeff;
    }
  
    public static Polynomial zero() {return new Polynomial(new long[0]);}
    public static Polynomial one() {return new Polynomial(new long[]{1L});}
    public static Polynomial constant(long c) {return new Polynomial(new long[]{c});}
    public static Polynomial x() {return new Polynomial(new long[]{0L, 1L});}
    public int degree() {return coeff.length - 1;}
    public long head() {return coeff[coeff.length - 1];}
    public long tail() {return coeff[0];}

    public Polynomial add(Polynomial x) {
        int len = coeff.length > x.coeff.length ? coeff.length : x.coeff.length;
        long[] c = new long[len];
        for (int i = 0; i < c.length; i++) {
            if (i < coeff.length) {
                if (i < x.coeff.length) {
                    c[i] = PF.add(coeff[i], x.coeff[i]);
                } else {
                    c[i] = coeff[i];
                }
             } else {
                c[i] = x.coeff[i];
             }
        }
        return new Polynomial(c);
    }
  
    public Polynomial sub(Polynomial x) {return add(x.mul(PF.neg(1L)));}
  
    public Polynomial mul(long x) {
        long[] c = new long[coeff.length];
        for (int i = 0; i < c.length; i++) c[i] = PF.mul(coeff[i], x);
        return new Polynomial(c);
    }
  
    public Polynomial mul(Polynomial x) {
        if (coeff.length == 0) return this;
        if (x.coeff.length == 0) return x;
        long[] c = new long[coeff.length + x.coeff.length - 1];
        for (int i = 0; i < c.length; i++) c[i] = 0L;

        for (int i = 0; i < coeff.length; i++) {
            for (int j = 0; j < x.coeff.length; j++)
                c[i + j] = PF.add(c[i + j], PF.mul(coeff[i], x.coeff[j]));
        }
        return new Polynomial(c);
    }
  
    // returns [this/x, this%x]
    public Polynomial[] div(Polynomial divisor) throws Exception {
        Polynomial dividend = zero();
        Polynomial remainder = this;
    
        while (remainder.degree() >= divisor.degree()) {
            int k = remainder.degree() - divisor.degree();
      
            // dividend += m * x^k
            // remainder -= m * x^k * divisor
            // choose m such that high term of remainder is cancelled
            long m = PF.div(remainder.head(), divisor.head());
      
            Polynomial z = x().pow(k);
      
            dividend = dividend.add(z.mul(m));
            remainder = remainder.sub(z.mul(m).mul(divisor));
        }
        return new Polynomial[]{dividend, remainder};
    }

    // compute this^e
    public Polynomial pow(int e) {
        Polynomial x = this;
        Polynomial r = one();
        while (e > 0) {
            if ((e & 1) != 0) r = r.mul(x);
            e >>= 1;
            x = x.mul(x);
        }
        return r;
    }

    // compute this^e mod p
    public Polynomial modPow(long e, Polynomial p) throws Exception {
        Polynomial x = this;
        Polynomial r = one();
        while (e > 0) {
            if ((e & 1L) != 0) r = r.mul(x).div(p)[1]; // r *= x mod p
            e >>= 1;
            x = x.mul(x).div(p)[1];  // x = x^2 mod p
        }
        return r;
    }
  
    public Polynomial monic() throws Exception {
        if (coeff.length == 0) return this;  // 0
        if (head() == 1L) return this; // already monic
        return mul(PF.inv(head()));
    }
  
    public Polynomial gcd(Polynomial x) throws Exception {
        Polynomial a = monic();
        Polynomial b = x.monic();
    
        while (true) {
            if (a.degree() < b.degree()) {
                Polynomial t = a;
                a = b;
                b = t;
            }
            if (b.degree() < 0) return a;   // gcd(a,0) == a
            Polynomial z = x().pow(a.degree() - b.degree());
            a = a.sub(b.mul(z)).monic();    // cancel the highest degree of a
        }
    }
  
    // Compute symbolic differential of the polynomial
    public Polynomial diff() {
        if (coeff.length == 0) return this;
        long[] c = new long[coeff.length - 1];
        for (int i = 1; i < coeff.length; i++)
            c[i - 1] = PF.mul(PF.nth(i), coeff[i]);
        return new Polynomial(c);
    }

    public long[] roots() throws Exception {
        ArrayList<Polynomial> lf = this.factor(1); // factorize into monomials
        // check factors are all monomials, eliminate any constants
        int constants = 0;
        for (Polynomial f : lf) {
            int x = f.degree();
            if (x > 1) return new long[0];
            if (x < 1) constants++;
        }
        long[] r = new long[lf.size() - constants];
        int i = 0;
        for (Polynomial f: lf)
            if (f.degree() == 1) r[i++] = PF.div(PF.neg(f.tail()), f.head());
        return r;
    }
       
    public ArrayList<Polynomial> factor(int maxdegree) throws Exception {
        // zero
        if (coeff.length == 0) {
            ArrayList<Polynomial> factors = new ArrayList<Polynomial>();
            factors.add(zero());
            return factors;
        }
    
        // constants
        if (coeff.length == 1) {
            ArrayList<Polynomial> factors = new ArrayList<Polynomial>();
            factors.add(constant(coeff[0]));
            return factors;
        }
    
        // linear terms
        if (coeff.length == 2) {
            ArrayList<Polynomial> factors = new ArrayList<Polynomial>();
            if (coeff[1] == 1L) {
                factors.add(this);
            } else {
                factors.add(constant(coeff[1]));
                factors.add(monic());
            }
            return factors;
        }
    
        // call factor2 with a monic polynomial of degree >= 2.
        if (head() == 1L) {
            return factor2(maxdegree);
        } else {
            ArrayList<Polynomial> factors = monic().factor2(maxdegree);
            factors.add(constant(head()));
            return factors;
        }
    }
  
    // polynomial must have degree >= 2 and be monic
    private ArrayList<Polynomial> factor2(int maxdegree) throws Exception {   
        // look for a repeated factor
        Polynomial f = gcd(diff());
        if (f.degree() > 0) {
            Polynomial f1 = f;
            Polynomial f2 = div(f)[0];
            ArrayList<Polynomial> factors = f1.factor(maxdegree);
            factors.addAll(f2.factor(maxdegree));
            return factors;
        }  
        return factor3(maxdegree);
    }
  
    // polynomial must have degree >= 2, be monic, and be square free
    private ArrayList<Polynomial> factor3(int maxdegree) throws Exception { 
        // http://planetmath.org/encyclopedia/DistinctDegreeFactorization.html
        Polynomial p = this;
        ArrayList<Polynomial> factors = new ArrayList<Polynomial>();
    
        // x^(q^k) - x mod p contains all the degree-k irreducible polys, q = |F|
        // We start it off with k == 0 which gives xqkx = 0.
        Polynomial xqkx = zero();
    
        // try all degrees which might be a factor of p.
        // (if p is reducible, it must have a factor of degree <= p.degree()/2)
        // use may have a maxdegree specified that is smaller
        int md = p.degree()/2;
        md = maxdegree < md ? maxdegree : md; 
        for (int k = 1; k <= md; k++) {
            // compute xqkx from previous value
            // - add x
            // - raise to the qth power x^(q^k) = (x^(q^(k-1)))^q
            // - subtract x
            xqkx = xqkx.add(x()).modPow(PF.order(), p).sub(x());
  
            // compute gcd with p
            Polynomial d = p.gcd(xqkx);
            if (d.degree() > 0) {
                if (d.degree() == k) {          // exactly one factor of degree k
                    factors.add(d);
                } else {                        // several factors of degree k
                    factors.addAll(d.factor4(k));
                }
                p = p.div(d)[0];
                if (p.degree() == 0) return factors;
            }
        }   
        // remaining polynomial must be irreducible
        factors.add(p);
        return factors;
    }

    // polynomial must have degree >= 2d, be monic, square free, and factor into
    // irreducible factors of degree exactly d.
    private ArrayList<Polynomial> factor4(int d) throws Exception {
        // Cantor-Zassenhaus algorithm
        Random r = new Random();
        Polynomial p = this;
        long e = PF.sub(PF.pow(PF.order(), d), 1L) / 2;
            
        while (true) {
            // pick a random t of degree < 2d
            long[] elts = new long[2*d];
            for (int i = 0; i < elts.length; i++) {
                elts[i] = PF.random(r);
            }
            Polynomial t = new Polynomial(elts);
          
            // compute t^((q^d - 1) / 2) - 1 mod p
            t = t.modPow(e, p).sub(one());
            
            // hopefully t and p have a factor in common
            Polynomial f = p.gcd(t);
            if (f.degree() > 0 && f.degree() < p.degree()) {  // found a factor of p
                Polynomial f1 = f;
                Polynomial f2 = p.div(f)[0];
        
                ArrayList<Polynomial> factors = new ArrayList<Polynomial>();
                if (f1.degree() == d) {
                    factors.add(f1);
                } else {
                    factors.addAll(f1.factor4(d));
                }
                if (f2.degree() == d) {
                    factors.add(f2);
                } else {
                    factors.addAll(f2.factor4(d));
                }
                return factors;
            }
        }
    }
}
