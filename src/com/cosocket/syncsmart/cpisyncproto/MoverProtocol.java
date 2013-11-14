package com.cosocket.syncsmart.cpisyncproto;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.cosocket.syncsmart.cpisyncproto.ItemPDU;
import com.cosocket.syncsmart.cpisyncproto.MoveSet;
import com.cosocket.syncsmart.cpisyncproto.MoverInterface;
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
 * A naive stateless implementation of MoverInterface.  
 * XXX: Much further work (protocol design, implementation, and testing)
 * is needed to support broadcast communications in a multi-party environment. 
 * The challenges remaining to be addressed are:
 * (i) suppression of repeated transmissions of the same item
 * (ii) support for large items if desired
 * (iii) addition of source coding, e.g., fountain codes to recover from losses 
 * 
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 * @param <V> type of items in set to be reconciled, must be Serializable
 */
public class MoverProtocol<V extends Serializable> implements MoverInterface<V> {
    private static final int INQSZ = 150;
    private static final int OUTQSZ = 150;
    private static final int MVQSZ = 20;
    private static final long OFFERMS = 50L;
    private ArrayBlockingQueue<MoveSet> pqin  = new ArrayBlockingQueue<MoveSet>(MVQSZ);   // from sync proto
    private ArrayBlockingQueue<ItemPDU<V>> iqin  = new ArrayBlockingQueue<ItemPDU<V>>(INQSZ);  // items from net
    private ArrayBlockingQueue<ItemPDU<V>> iqout = new ArrayBlockingQueue<ItemPDU<V>>(OUTQSZ);  // items to net
    private ItemProcessor itemProc = new ItemProcessor();
    private MoveSetProcessor moveProc = new MoveSetProcessor();
    private StoreInterface<V> store;
    protected boolean stopAll = false;
    
    public MoverProtocol(StoreInterface<V> store) {
        this.store = store;
        itemProc.start();
        moveProc.start();
    }
    
    public StoreInterface<V>         getStore()             {return store;}
    public boolean                   enquePDU(ItemPDU<V> m) {return iqin.offer(m);}     
    public ItemPDU<V>                dequePDU()             {return iqout.poll();}
    
    public void moveAll(MoveSet p) {   
        pqin.offer(p);
    }
    
    public void stop() {
        itemProc.interrupt();
        moveProc.interrupt();
        pqin.clear();
        iqin.clear();
        iqout.clear();
        stopAll = true;
    }

    
    private void send(ItemPDU<V> m) {
        try {
            iqout.offer(m, OFFERMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) { 
        }
        // suppress ??
    }

    private class MoveSetProcessor extends Thread {
        public void run() {
            setName("MoveSet Processor Thread");
            while (stopAll == false) {
                try {
                    MoveSet p = pqin.take();
                    for(long tag : p.dataArray) send(new ItemPDU<V> (p.from, p.to, store.getSetID(), tag, store.getValue(tag)));
                    // pack more into each PDU? fragment across PDUs? use TCP? use fountain codes? 
                    // what if store has a filename instead of memory object
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private class ItemProcessor extends Thread {
        public void run() {
            setName("Item Processor Thread");
            while (stopAll == false) {
                try {
                    recv(iqin.take());
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
      
    private void recv(ItemPDU<V> m) throws Exception {   
        if (!store.sameSet(m.getSetID())) return;
        store.addIfNew(m.getKey(), m.getItem());
        System.out.println("Added " + m.getKey());
    }
}
