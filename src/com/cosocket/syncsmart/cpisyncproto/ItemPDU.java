package com.cosocket.syncsmart.cpisyncproto;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import com.cosocket.syncsmart.cpisyncproto.SyncPeer;

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
 * The ItemPDU encapsulates an item from the set to be reconciled for queueing and transmission 
 * Methods to serialize and deserialize into a ByteBuffer for transmission are provided.
 * The PDU includes a MAGIC string and version number, the from and to SyncPeer instances, 
 * the setID identifying the set being reconciled, the key of the item drawn from a finite 
 * field used by the reconciliation algorithm, and the item itself. Variable length fields
 * are prefixed with a 32bit length.
 * @author Dr. Rajesh Krishnan (krash@cosocket.com)
 *
 * @param <V> is any Serializable object that can fit within a datagram on the space remaining after the other protocol fields
 */
public class ItemPDU<V extends Serializable> {
    public static final byte[] MAGIC = new String("SYNCDATA").getBytes();
    public static final byte version = 1;
    private SyncPeer from;
    private SyncPeer to;
    private byte[] setID;
    private long tag;
    private V item;

    public SyncPeer getFrom() {return from;}
    public SyncPeer getTo() {return to;}
    public byte[] getSetID() {return setID;}
    public long getKey() {return tag;}
    public V getItem() {return item;}

    public ItemPDU(SyncPeer from, SyncPeer to, byte[] setID, long tag, V item) {
        this.from = from;
        this.to = to;
        this.setID = setID;
        this.tag = tag;
        this.item = item;
    }
       
    @SuppressWarnings("unchecked")
    public ItemPDU<V> fromBuffer(ByteBuffer buf) throws Exception {        
        SyncPeer from;
        SyncPeer to;
        byte[]   setID;
        long     tag;
        
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
        tag           = buf.getLong();
        i             = buf.getInt();        
        byte[] itemBytes = new byte[i];
        buf.get(itemBytes, 0, i);
        
        ByteArrayInputStream bis = new ByteArrayInputStream(itemBytes);
        ObjectInput in = null;
        Object item;
        try {
          in = new ObjectInputStream(bis);
          item = in.readObject(); 
          bis.close();
          in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        this.from = from;
        this.to = to;
        this.setID = setID;
        this.tag = tag;
        this.item = (V) item;
        return this;
    }
    
    public boolean toBuffer(ByteBuffer buf) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] itemBytes = null;
        try {
            out = new ObjectOutputStream(bos);   
            out.writeObject(item);
            itemBytes = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();        
            return false;
        }
        
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
        buf.putLong(tag);
        buf.putInt(itemBytes.length);
        buf.put(itemBytes, 0, itemBytes.length);
        buf.flip();
        return true;
    }
}
