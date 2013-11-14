package com.cosocket.syncsmart.cpisyncproto;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
 * Class that has information about the network location of the peers engaged
 * in the set reconciliation protocol. The reasons not to just use InetSocketAddress
 * are (i) to support peers that may not be IP (e.g., URLs, Ethernet addresses), and 
 * (ii) to provide convenient methods for the mrpoe abstract sync protocol to test SyncPeer 
 * instances for equality (e.g., is this intended for me) and to test for group communication
 * (e.g., is this a broadcast or multicast address) without IP-specifics
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
public class SyncPeer extends InetSocketAddress {
    private static final long serialVersionUID = 1L;
    public SyncPeer(InetAddress addr, int port) {super(addr, port);}
    public boolean equals(SyncPeer c) {return super.equals(c);}
    public boolean anyHost() {
        InetAddress i = getAddress();
        return i.isAnyLocalAddress() || i.isMulticastAddress();
    }
}
