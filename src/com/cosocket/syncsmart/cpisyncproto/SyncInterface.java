package com.cosocket.syncsmart.cpisyncproto;
import java.io.Serializable;
import com.cosocket.syncsmart.cpisyncproto.SyncPDU;
import com.cosocket.syncsmart.cpisyncproto.StoreInterface;
import com.cosocket.syncsmart.cpisyncproto.MoverInterface;
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
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 * @param <V>
 */
public interface SyncInterface<V extends Serializable> {
    /**
     * Function to acccess the StoreInterface that this SyncInterface is associated with
     * @return the associated StoreInterface
     */
    public StoreInterface<V>      getStore();
    /**
     * Function to acccess the MoverInterface that this SyncInterface is associated with
     * @return the associated MoverInterface
     */
    public MoverInterface<V>      getMover();
    /**
     * Attempts to enqueue by offering a SyncPDU and returns immediately
     * @param m instance of SyncPDU, typically from a deserialized datagram
     * @return true if the queueing was successful, false otherwise 
     */
    public boolean                enquePDU(SyncPDU m);
    /**
     * Attempts to dequeue by polling for a SyncPDU and returns immediately
     * @return instance of SyncPDU or null if queue was empty
     */
    public SyncPDU                dequePDU();
    /**
     * Interrupts threads that process the SyncPDU queues and empties them.
     * Cancels the internal protocol timer and also calls stop on the associated MoverInterface.
     */
    public void                   stop();
}
