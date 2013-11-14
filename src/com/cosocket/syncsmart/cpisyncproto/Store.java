package com.cosocket.syncsmart.cpisyncproto;
import java.io.Serializable;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import com.cosocket.syncsmart.cpisync.Reconciler;
import com.cosocket.syncsmart.cpisyncproto.StoreInterface;
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
 * An implementation of the StoreInterface using ConcurrentHashMap
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 * @param <V> type of items in set to be reconciled, must be Serializable
 */
public class Store<V extends Serializable> extends ConcurrentHashMap<Long,V> implements StoreInterface<V> {
    private static final long serialVersionUID = 1L;
    private byte[]   setID;
    public  Store (byte[] setID)                     {this.setID = setID;}
    public  byte[]   getSetID()                      {return setID;}
    public  boolean  sameSet(byte[] otherID)         {return Arrays.equals(setID, otherID);}
    public  int      cardinality()                   {return size();}
    public  V        getValue(long key)              {return get(key);}
    public  V        removeValue(long key)           {return remove(key);}
    public  long[]   keySetArray(long from, long to) {
        TreeSet<Long> t = new TreeSet<Long>(keySet());
        if (from == 0 && to == Reconciler.getDatamax()) return Reconciler.toArray(t);
        return Reconciler.toArray(t.subSet(from, to));
    }
    public  V        addIfNew(long key, V value) throws Exception {
        if (!Reconciler.validKey(key)) throw new Exception("Key not in range");
        V old = putIfAbsent(key, value);
        if((old != null) && !old.equals(value)) throw new Exception("Key collision");
        return old;
    }
}
