package com.cosocket.syncsmart.cpisyncproto;
import java.io.Serializable;
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
 * Interface to a key-value store that contains items of set to be reconciled
 * Typically this interface will be associated with interfaces to a set reconciliation protocol
 * and a protocol that transfers the items over a network. It is envisioned that implementations 
 * will use concurrent data structures.
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 * @param <V> type of items in set to be reconciled; must be Serializable
 */
public interface StoreInterface<V extends Serializable> {
    /**
     * Retuen the identification of the set to be reconciled
     * @return the setID of the set to be reconciled 
     */
    public byte[]  getSetID();
    /**
     * Compare whether a given set identifier matches the set serviced by this StoreInterface
     * @param otherID the set identifier
     * @return true if the set identifiers match, false otherwise
     */
    public boolean sameSet(byte[] otherID);
    /**
     * Returns the size of the set serviced by this StoreInterface
     * @return the number of items in the set to be reconciled
     */
    public int     cardinality();
    /**
     * Returns a subset of the keys of set serviced by this StoreInterface that fall in a specified range
     * @param from the minimum (inclusive) key in the subset
     * @param to the maximum (exclusive) key in the subset
     * @return the computed subset that are in [from,to)
     */
    public long[]  keySetArray(long from, long to);
    /**
     * Retrieve (do not remove) the item corresponding to the key from the set serviced by the StoreInterface
     * @param key the key of the item to retrieve 
     * @return the value corresponding to the key, null if not found
     */
    public V       getValue(long key);
    /**
     * Delete the key-value pair corresponding to the specified key from the set serviced by the StoreInterface
     * @param key the key of the item to delete
     * @return the value corresponding to the key, null if not found
     */
    public V       removeValue(long key);
    /**
     * Atomically add to the set serviced by this StoreInterface, the provided key-value pair if the key
     * is not already bound. Return the prior value if the key is already bound.   
     * 
     * @param key the key of the item to be tested and added
     * @param value the corresponding value to be added
     * @return prior value the key is bound to, null if item is new
     * @throws Exception any exceptions reported by the underlying implementation, e.g., for attempting to insert nulls
     */
    public V       addIfNew(long key, V value) throws Exception;
}
