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
 * Interface to a protocol that moves metadata between distributed peers that have a set to be reconciled
 * Typically an associated sync protocol will provide the items to be moved, and an associated transport
 * will service the queues. 
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 * @param <V> Type of item in sets to be reconciled and accessed via associated StoreInterface
 */
public interface MoverInterface<V extends Serializable> {
    /**
     * Function to acccess the StoreInterface that this MoverInterface is associated with
     * @return the associated StoreInterface
     */
    public StoreInterface<V>         getStore();
    /**
     * Attempts to enqueue by offering an ItemPDU<V> and returns immediately
     * @param m instance of ItemPDU<v>, typically from a deserialized datagram
     * @return true if the queueing was successful, false otherwise 
     */
    public boolean                   enquePDU(ItemPDU<V> m);
    /**
     * Attempts to dequeue by polling for an ItemPDU<V> and returns immediately
     * @return instance of ItemPDU<V> or null if queue was empty
     */
    public ItemPDU<V>                dequePDU();
    /**
     * Enqueues a MoveSet identifying all the elements to be transferred
     * @param p MoveSet includes dataArray of keys for items to be moved, SyncPeer from, and SyncPeer to 
     */
    public void                      moveAll(MoveSet p);
    /**
     * Interrupts threads that process the ItemPDU queues and MoveSet queue and empties them.
     * 
     */
    public void                      stop();
}
