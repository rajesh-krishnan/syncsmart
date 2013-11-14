package com.cosocket.syncsmart.cpisyncproto;
import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import com.cosocket.syncsmart.cpisync.Partition;
import com.cosocket.syncsmart.cpisync.Reconciler;
import com.cosocket.syncsmart.cpisyncproto.MoveSet;
import com.cosocket.syncsmart.cpisyncproto.SyncPDU;
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
 * An implementation of SyncInterface
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 * @param <V> type of items in set to be reconciled, must be Serializable
 */
public class SyncProtocol<V extends Serializable> implements SyncInterface<V> {
    private static final long INITSYNC = 5000;
    private static final long TIMEOUT = 2 * 60 * 1000;
    private static final int INQSZ = 20;
    private static final int OUTQSZ = 20;
    private static final long bulkThresh = 1024;

    protected SyncPeer self;
    protected SyncPeer other;
    protected StoreInterface<V> stor;
    protected MoverInterface<V> mover;
    private   Thread syncThread;
    private   Timer timer = new Timer();
    private   TimerTask ttask = null;
    private   ArrayBlockingQueue<SyncPDU> sqin = new ArrayBlockingQueue<SyncPDU>(INQSZ);
    private   ArrayBlockingQueue<SyncPDU> sqout = new ArrayBlockingQueue<SyncPDU>(OUTQSZ);
    protected boolean stopAll = false;
    
    public SyncProtocol(SyncPeer self, SyncPeer other, StoreInterface<V> stor, MoverInterface<V> mover) throws Exception {
        this.self = self;
        this.other = other;
        this.stor = stor;
        this.mover = mover;
        if(!mover.getStore().sameSet(stor.getSetID())) throw new Exception("Mover set mismatch");
        resetTimer(INITSYNC);
        syncThread = new Thread() {
            public void run() {
                this.setName("Sync Thread");
                while (stopAll == false) {
                    try {
                        recv(sqin.take());
                    } catch (InterruptedException e) {
                        System.out.println("Sync Thread Interrupted");
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        syncThread.start();
    }
    
    public StoreInterface<V>      getStore()          {return stor;}
    public MoverInterface<V>      getMover()          {return mover;}
    public boolean                enquePDU(SyncPDU m) {return sqin.offer(m);}
    public SyncPDU                dequePDU()          {return sqout.poll();}

    public void stop() {
        timer.cancel();
        sqin.clear();
        sqout.clear();
        mover.stop();
        syncThread.interrupt();
        stopAll = true;
    }

    private void resetTimer(long timeout) {
        ttask = new TimerTask() {
            public void run() {
                System.out.println("timer fired, store has: " + stor.cardinality());
                try {
                    onTimer();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        };
        timer.schedule(ttask, timeout);
    }

    private void cancelTimerTask() {
        if(ttask != null) ttask.cancel();
        timer.purge();
    }

    private void onTimer() throws Exception {
        long[] part = Partition.root(0, Reconciler.getDatamax());
        send(requestSyncMessage(other, part));
        System.out.println("sync queue " + sqout.size());
        resetTimer(TIMEOUT);
    }

    private void send(SyncPDU m) {
        sqout.offer(m);
    }

    private void recv(SyncPDU m) {
        cancelTimerTask();
        try {
            byte type = m.getType();
            boolean sv = false;
            switch (type) {
                case SyncPDU.REQALL:
                    respondSendAllItems(m);
                    break;
                case SyncPDU.REQ:
                    respondSendRequestedItems(m);
                    break;
                case SyncPDU.SV:
                    sv = true;
                case SyncPDU.CPI:
                    handle(sv, m);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetTimer(TIMEOUT);
    }

    private void handle(boolean sv, SyncPDU m) throws Exception {
        SyncPeer f = m.getFrom();
        SyncPeer t = m.getTo();
        byte[] sid = m.getSetID();
        int mB     = m.getSetsize();
        long begin = m.getBegin();
        long end   = m.getEnd();
        long[] eB  = m.getRecVector();
        
        if (!(t.equals(self) || t.anyHost())) {System.out.println ("not for me"); return;}
        if (!stor.sameSet(sid)) {System.out.println("not same set"); return;}

        long[] dA  = stor.keySetArray(begin, end);
        int mA     = dA.length;
        long[] eA  = Reconciler.syncArray(sv, dA);
               
        Vector<long[]> deltas = new Vector<long[]>();
        boolean success       = Reconciler.reconcile(sv, eA, eB, mA, mB, deltas);
        System.out.println("SV: "  + sv  + " " +
                "eA: "  + eA.length + " " + "eB: "  + eB.length + " " +
                "mA: "  + mA + " " + "mB: "  + mB + " " +
                "success: " + success + " " + "deltas : " + 
                (success ? deltas.get(0).length + "," + deltas.get(1).length : "0,0"));
        
        if (success) handleDeltas(f, deltas.get(0), deltas.get(1));
        deltas.clear();       
        
        if (!success && end - begin <= bulkThresh) { // go postal
            System.out.println("going bulk");
            goBulk(f, begin, end);
            success = true;
        }

        long[] n = Partition.partition(begin, end, 0, Reconciler.getDatamax());
        Partition.next(success, n, n);
        SyncPeer sndc = t.anyHost() ? t : f;
        if (!Partition.isDone(n)) send(requestSyncMessage(sndc, n));       
    }

    private SyncPDU requestSyncMessage(SyncPeer other, long[] part) {
        long begin = Partition.begin(part);
        long end = Partition.end(part);
        long[] data = stor.keySetArray(begin, end);
        int setsize = data.length;
        boolean sv = Reconciler.isSV(setsize, begin, end);
        byte type = sv ? SyncPDU.SV : SyncPDU.CPI;
        long[] syncarr = Reconciler.syncArray(sv, data);
        return new SyncPDU(self, other, stor.getSetID(), type, setsize, begin, end, syncarr);
    }

    private void handleDeltas(SyncPeer other, long[] push, long[] pull) throws Exception {
        mover.moveAll(new MoveSet(self, other, push));
        send(reqItemsMessage(other, pull));
    }

    private SyncPDU reqItemsMessage(SyncPeer other, long[] deltas) throws Exception {
        return new SyncPDU(self, other, stor.getSetID(), SyncPDU.REQ, 0, 0, Reconciler.getDatamax(), deltas);
    }

    private void respondSendRequestedItems(SyncPDU m) throws Exception {
        mover.moveAll(new MoveSet(self, m.getTo(), m.getRecVector()));
    }

    private void goBulk(SyncPeer other, long begin, long end) throws Exception {
        mover.moveAll(new MoveSet(self, other, stor.keySetArray(begin, end)));
        send(requestAllItemsMessage(other, begin, end));
    }

    private SyncPDU requestAllItemsMessage(SyncPeer other, long begin, long end) throws Exception {
        return new SyncPDU(self, other, stor.getSetID(), SyncPDU.REQALL, 0, begin, end, null);
    }

    private void respondSendAllItems(SyncPDU m) throws Exception {
        mover.moveAll(new MoveSet(self, m.getTo(), stor.keySetArray(m.getBegin(), m.getEnd())));
    }
}
