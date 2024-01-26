package com.example.wifipocv2;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A VirtualPacket represents a packet being sent over the virtual mesh network. The packet data
 * includes a header (which contains the virtual to/from address and port, hop count, max hops, and
 * payload size) followed by a payload of a given length in bytes as specified within the header.
 * <p>
 * The payloadOffset is always >= VirtualPacketHeader.HEADER_SIZE - this makes it possible to convert
 * to/from a DatagramPacket without requiring a new buffer (e.g. the offset can be moved as required).
 *
 * @param data       ByteArray to be used to store data
 * @param dataOffset the offset to the start of the data (the header data begins at offset, payload
 *                   data begins at dataOffset + VirtualPacketHeader.HEADER_SIZE)
 * @param header     if the header is supplied - it will be written into the data. If the header is not
 *                   supplied, the header data MUST be in data and it will be read when constructed
 */
public class VirtualPacket {

    private final byte[] data;
    private final int dataOffset;
    private final VirtualPacketHeader header;
    /**
     * Broadcast address - 255.255.255.255 (-1)
     */
    public static final int ADDR_BROADCAST = (255 << 24) | (255 << 16) | (255 << 8) | 255;
    private int LAST_HOP_ADDR_OFFSET = 12;


    public VirtualPacket(byte[] data, int dataOffset, VirtualPacketHeader header, boolean assertHeaderAlreadyInData) {
        this.data = data;
        this.dataOffset = dataOffset;
        this.header = (header != null) ? header : VirtualPacketHeader.fromBytes(data, dataOffset);

        if (!assertHeaderAlreadyInData) {
            this.header.toBytes(data, dataOffset);
        }
    }

    public int getPayloadOffset() {
        return dataOffset + VirtualPacketHeader.HEADER_SIZE;
    }

    /**
     * The size required for this packet when converted into a datagram packet
     */
    public int getDatagramPacketSize() {
        return header.getPayloadSize() + VirtualPacketHeader.HEADER_SIZE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VirtualPacket)) return false;

        VirtualPacket other = (VirtualPacket) obj;
        return header.equals(other.header) && Arrays.equals(data, other.data) && dataOffset == other.dataOffset;
    }

    public DatagramPacket toDatagramPacket() {
        return new DatagramPacket(data, dataOffset, getDatagramPacketSize());
    }

    @Override
    public int hashCode() {
        int result = header.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + dataOffset;
        return result;
    }

    /**
     * Update the data array to set the last hop address and increment hop count. This would typically
     * be called by the route function just before the packet is sent.
     *
     * @param lastHopAddr the value to set for the last hop address
     */
    void updateLastHopAddrAndIncrementHopCountInData(int lastHopAddr) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data, dataOffset + LAST_HOP_ADDR_OFFSET, 5);
        byteBuffer.putInt(lastHopAddr);
        byteBuffer.put((byte) (header.getHopCount() + 1));
    }

    public boolean isBroadcast() {
        return header.getToAddr() == ADDR_BROADCAST;
    }

    public static final int MAX_PAYLOAD_SIZE = 1500;
    public static final int VIRTUAL_PACKET_BUF_SIZE = MAX_PAYLOAD_SIZE + VirtualPacketHeader.HEADER_SIZE;
}

