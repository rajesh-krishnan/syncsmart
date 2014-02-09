package com.cosocket.syncsmart.cpisyncproto;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.net.InetSocketAddress;

/*
Copyright (c) 2014, Cosocket LLC
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
 * This class is an experimental quick-and-dirty UDP-based transport for smartsync.
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 * 
 * @param <V> type of the Serializable items to be reconciled, accessed via a StoreInterface
 */
public class DatagramTransport<V extends Serializable> {
    private DatagramChannel channel;
    private SyncInterface<V> sync;
    private MoverInterface<V> mover;
    private Rcvr rcvThread      = new Rcvr();
    private Sndr sndThread        = new Sndr();
    private boolean stopAll         = false;
    
    /**
     * Constructor opens a DatagramChannel, binds to the specified address and port, starts the threads that send
     * and receive datagrams in order to service the associated SyncInterface and MoverInterface queues 
     * 
     * @param bind  InetSocketAddress to bind the UDP server
     * @param sync  the SyncInterface to a set reconciliation protocol, which in turn accesses a StoreInterface with the items
     * @param mover the MoverInterface to a protocol to move items also accessed through the same StoreInterface instance
     */
    public DatagramTransport(InetSocketAddress bind, SyncInterface<V> sync, MoverInterface<V> mover) {
        try {
            channel = DatagramChannel.open();
            channel.socket().bind(bind);
            channel.socket().setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.sync = sync;
        this.mover = mover;
        rcvThread.start();
        sndThread.start();
    }    
    
    /**
     * Invokes stop on the associated StoreInterface and MoverInterface, interrupts the threads that service the queues
     * from those interfaces, and closes the associated DatagramChannel
     */
    public void stop() {
        sync.stop();
        mover.stop();
        rcvThread.interrupt();
        sndThread.interrupt();
        try { channel.close(); } catch (IOException e) {e.printStackTrace();}
    }
       
    private class Rcvr extends Thread {
        private ByteBuffer rcvData = ByteBuffer.allocate(1600);
        public void run() {
            this.setName("Rcvr Thread");
            while (stopAll == false && channel.isOpen()) {
                try {  
                    rcvData.clear();
                    InetSocketAddress rc = (InetSocketAddress) channel.receive(rcvData);
                    rcvData.flip();
                    if (rc != null) {
                        if(SyncPDU.isSyncPDU(rcvData)) {
                            SyncPDU s = SyncPDU.fromBuffer(rcvData);
                            if (s != null) sync.enquePDU(s);
                        } else {
                            ItemPDU<V> s = (new ItemPDU<V>(null,null,null,0,null)).fromBuffer(rcvData);
                            if (s != null) mover.enquePDU(s);
                        }
                    }

                } catch (ClosedByInterruptException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
        
    private class Sndr extends Thread {
        private ByteBuffer sndData = ByteBuffer.allocate(1600);
        public void run() {
            while (stopAll == false && channel.isOpen()) {
                this.setName("Sndr Thread");
                try {
                    sndData.clear();
                    SyncPDU s = sync.dequePDU();
                    if(s != null && s.toBuffer(sndData)) {
                        channel.send(sndData, s.getTo());
                    }
                    sndData.clear();
                    ItemPDU<V> i = mover.dequePDU();
                    if(i != null && i.toBuffer(sndData)) {
                        channel.send(sndData, i.getTo());
                    }
                } catch (ClosedByInterruptException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
