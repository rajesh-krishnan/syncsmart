package com.cosocket.syncsmart.cpisyncproto;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
 * The SyncPDU encapsulates a control message used by the set reconciliation protocol
 * for queueing and transmission. Methods to serialize and deserialize into a ByteBuffer 
 * for transmission are provided. The PDU includes a MAGIC string and version number, the 
 * type of the message, the from and to SyncPeer instances, the setID identifying the set being
 * reconciled, the size of the set (or partition) being reconciled at the sender, the beginning 
 * (inclusive) and end (exclusive) of the partition, and either the summary vector or sync 
 * vector as applicable. Variable length fields are prefixed with a 32 bit length for the 
 * datatype. The SyncPDU type can be REQALL (send all items in range), REQ (send only items 
 * whose keys are provided), SV (summary vector), CPI (vector containing evaluations of the 
 * characteristic polynomial at known sample points).  The setsize field is relevant only for CPI.
 * 
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 */
public class SyncPDU {
    public static final byte[] MAGIC = new String("SYNCCTRL").getBytes();
    public static final byte version = 1;
    public static final byte REQALL  = 0;   // request to pull all items in range, no hashes sent
    public static final byte REQ     = 1;   // request to pull items for included hashes
    public static final byte SV      = 2;   // summary vector for items in partition
    public static final byte CPI     = 3;   // CPI vector for items in partition

    private byte     type;
    private SyncPeer from;
    private SyncPeer to;
    private byte[]   setID;
    private int      setsize;
    private long     begin;
    private long     end;
    private long[]   recVector;
    
    public byte     getType()      {return type;}
    public SyncPeer getFrom()      {return from;}
    public SyncPeer getTo()        {return to;}
    public byte[]   getSetID()     {return setID;}
    public int      getSetsize()   {return setsize;}
    public long     getBegin()     {return begin;}
    public long     getEnd()       {return end;}
    public long[]   getRecVector() {return recVector;}
       
    public SyncPDU(SyncPeer from, SyncPeer to, byte[] setID, byte type, int setsize, long begin, long end, long[] recVector) {
        this.type      = type;
        this.from      = from;
        this.to        = to;
        this.setID     = setID;
        this.setsize   = setsize;
        this.begin     = begin;
        this.end       = end;
        this.recVector = recVector;        
    }
 
    public static boolean isSyncPDU(ByteBuffer buf) throws Exception {  
        ByteBuffer bb  = buf.duplicate();
        int i          = bb.getInt();
        byte[] magic   = new byte[i]; 
        bb.get(magic, 0, i);
        byte ver       = bb.get();
        return (Arrays.equals(MAGIC, magic) && version == ver);     
    }
    
    public static SyncPDU fromBuffer(ByteBuffer buf) throws Exception {        
        SyncPeer from;
        SyncPeer to;
        byte[]   setID;
        byte     type;
        int      setsize;
        long     begin;
        long     end;
        long[]   recVector;
        
        int i         = buf.getInt();
        byte[] magic  = new byte[i]; 
        buf.get(magic, 0, i);     
        if (!Arrays.equals(MAGIC, magic)) {System.out.println("Magic failure"); return null;}
        if (version != buf.get()) {System.out.println("Wrong version"); return null;}        
        i             = buf.getInt();
        byte[] faddr  = new byte[i];
        buf.get(faddr, 0, i);
        InetAddress f = InetAddress.getByAddress(faddr);
        int fport     = buf.getInt();       
        from          = new SyncPeer(f, fport);
        i             = buf.getInt();
        byte[] taddr  = new byte[i];
        buf.get(taddr, 0, i);
        InetAddress t = InetAddress.getByAddress(taddr);
        int tport     = buf.getInt(); 
        to            = new SyncPeer(t, tport);
        i             = buf.getInt();
        setID         = new byte[i];
        buf.get(setID, 0, i);
        type          = buf.get();
        setsize       = buf.getInt();
        begin         = buf.getLong();
        end           = buf.getLong();
        i             = buf.getInt();        
        recVector     = new long[i];
        buf.asLongBuffer().get(recVector, 0, i);

        return new SyncPDU(from, to, setID, type, setsize, begin, end, recVector);
    }
    
    public boolean toBuffer(ByteBuffer buf) throws Exception {
        byte[] tmp;
        buf.clear();
        buf.putInt(MAGIC.length);
        buf.put(MAGIC, 0, MAGIC.length);
        buf.put(version);
        tmp = from.getAddress().getAddress();
        buf.putInt(tmp.length);
        buf.put(tmp);
        buf.putInt(from.getPort());
        tmp = to.getAddress().getAddress();
        buf.putInt(tmp.length);
        buf.put(tmp);
        buf.putInt(to.getPort());
        buf.putInt(setID.length);
        buf.put(setID, 0, setID.length);
        buf.put(type);
        buf.putInt(setsize);
        buf.putLong(begin);
        buf.putLong(end);
        buf.putInt(recVector.length);
        for (long l : recVector) buf.putLong(l);
        buf.flip();
        return true;
    }
}
