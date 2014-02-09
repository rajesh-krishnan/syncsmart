package com.cosocket.syncsmart;
import java.net.InetAddress;
import java.util.Random;
import com.cosocket.syncsmart.cpisync.Reconciler;
import com.cosocket.syncsmart.cpisyncproto.MoverInterface;
import com.cosocket.syncsmart.cpisyncproto.StoreInterface;
import com.cosocket.syncsmart.cpisyncproto.MoverProtocol;
import com.cosocket.syncsmart.cpisyncproto.Store;
import com.cosocket.syncsmart.cpisyncproto.SyncPeer;
import com.cosocket.syncsmart.cpisyncproto.SyncInterface;
import com.cosocket.syncsmart.cpisyncproto.SyncProtocol;
import com.cosocket.syncsmart.cpisyncproto.DatagramTransport;

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
 * This class implements a basic UDP-based test for the smartsync protocol.
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
public class UDPTest {
    /**
     * The main method tests the smartsync protocol.  Instantiates Store, SyncProtocol, MoverProtocol and
     * associates them. Populates the Store with random initial data. Creates a UDPTest protocol using the
     * SyncProtocol and MoverProtocol. The addresses and ports for (binding) self and the other peer(s) 
     * are accepted as arguments.  Currently only tested with unicast on same address and different ports. 
     * @param args myIP myPort otherIP otherPort
     * @throws Exception whenever any of the components throw an Exception
     */
    public static void main(String[] args) throws Exception {
        InetAddress addrSelf = null;
        InetAddress addrOther = null;
        int portSelf = 0;
        int portOther = 0;
        try {
            addrSelf = InetAddress.getByName(args[0]);
            portSelf = Integer.parseInt(args[1]);
            addrOther = InetAddress.getByName(args[2]);
            portOther = Integer.parseInt(args[3]);
        } catch (Exception e) { System.out.println("Usage: UDPTest myIP myPort toIP toPort"); System.exit(0);}

        String s = "METADATA";
        StoreInterface<String> stif = new Store<String>(s.getBytes());
        java.util.Random r = new Random(System.currentTimeMillis()); 
        for (long i = 0; i < 500; i++) {
            long j = Reconciler.randomKey(r);
            String sj = Long.toString(j);
            stif.addIfNew(Reconciler.hash(sj.getBytes()), sj);
        }
        MoverInterface<String> mvif = new MoverProtocol<String>(stif);

        SyncPeer self  = new SyncPeer(addrSelf, portSelf); 
        SyncPeer other = new SyncPeer(addrOther, portOther);
        SyncInterface<String> spif = new SyncProtocol<String>(self, other, stif, mvif);
        DatagramTransport<String> ut = new DatagramTransport<String>(self, spif, mvif);
        System.out.println("started UDPTest");
        
        try {Thread.sleep(600000);} catch (InterruptedException e) {}
        System.out.println("Store has: " + stif.cardinality());
        ut.stop();
        
        System.out.println("stopped UDPTest");
    }
}
