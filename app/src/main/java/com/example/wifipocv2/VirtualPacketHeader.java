package com.example.wifipocv2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Virtual packet structure
 * Header:
 * @param toAddr - the Virtual Node address this packet wants to reach. 0 = send over local link only.
 * @param port the Virtual Port on the destination Virtual Node that this packet wants to reach
 * @param fromAddr the Virtual Node that originally sent this packet
 * @param fromPort the virtual port that this packet was sent from
 * @param lastHopAddr the virtual node address of the most recent hop. E.g. where the packet is
 *        sent from node A to node B, then node B to node C, the lastHop would be A when the packet
 *        is sent from A to B, and then B when the packet is sent from B to C.
 * @param hopCount the total number of hops this packet has taken. Starts at 1 when first sent and
 *        is incremented on each hop.
 * @param maxHops the maximum number of hops that this packet should live for. If exceeded, packet is
 *        dropped
 * @param payloadSize the size of the payload data
 *
 * Packet size/structure:
 * To address (32bit int)
 * to port (16bit short)
 * from address (32bit int)
 * from port (16bit short)
 * lastHopAddr (32 bit int)
 * hopCount (8bit byte)
 * maxHops (8bit byte)
 * payloadSize (16bit short)
 * payload (byte array where size = payloadSize)
 */

public class VirtualPacketHeader {

    private final int toAddr;
    private final int toPort;
    private final int fromAddr;
    private final int fromPort;
    private final int lastHopAddr;
    private final byte hopCount;
    private final byte maxHops;
    private final int payloadSize; //Max size should be in line with MTU e.g. 1500. Stored as short

    public VirtualPacketHeader(
            int toAddr, int toPort, int fromAddr, int fromPort,
            int lastHopAddr, byte hopCount, byte maxHops, int payloadSize) {
        this.toAddr = toAddr;
        this.toPort = toPort;
        this.fromAddr = fromAddr;
        this.fromPort = fromPort;
        this.lastHopAddr = lastHopAddr;
        this.hopCount = hopCount;
        this.maxHops = maxHops;
        this.payloadSize = payloadSize;

        if (payloadSize > MAX_PAYLOAD) {
            throw new IllegalArgumentException("Payload size must not be > " + MAX_PAYLOAD);
        }
    }

    public int getPayloadSize(){
        return this.payloadSize;
    }

    public int getHopCount(){
        return this.hopCount;
    }

    public int getToAddr(){
        return this.toAddr;
    }

    public int getLastHopAddr(){
        return this.lastHopAddr;
    }

    public void toBytes(byte[] byteArray, int offset) {
        ByteBuffer buf = ByteBuffer.wrap(byteArray, offset, HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(toAddr);
        buf.putShort((short) toPort);
        buf.putInt(fromAddr);
        buf.putShort((short) fromPort);
        buf.putInt(lastHopAddr);
        buf.put(hopCount);
        buf.put(maxHops);
        buf.putShort((short) payloadSize);
    }

    public byte[] toBytes() {
        byte[] result = new byte[HEADER_SIZE];
        toBytes(result, 0);
        return result;
    }

    public static VirtualPacketHeader fromBytes(byte[] bytes, int offset) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buf.position(offset);
        int toAddr = buf.getInt();
        int toPort = buf.getShort() & 0xFFFF; // Convert to unsigned short
        int fromAddr = buf.getInt();
        int fromPort = buf.getShort() & 0xFFFF; // Convert to unsigned short
        int lastHopAddr = buf.getInt();
        byte hopCount = buf.get();
        byte maxHops = buf.get();
        int payloadSize = buf.getShort() & 0xFFFF; // Convert to unsigned short

        return new VirtualPacketHeader(toAddr, toPort, fromAddr, fromPort, lastHopAddr, hopCount, maxHops, payloadSize);
    }

    // Size of all header fields in bytes (as above)
    public static final int HEADER_SIZE = 20;
    public static final int MAX_PAYLOAD = 2000;
}

