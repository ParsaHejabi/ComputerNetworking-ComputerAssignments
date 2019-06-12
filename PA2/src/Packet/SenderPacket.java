package Packet;

import java.util.Random;

public class SenderPacket extends Packet {
    private static final int PACKET_SIZE = 512;

    private final int sequenceNumber;
    private final byte[] data;

    public SenderPacket(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        this.data = new byte[PACKET_SIZE];

        // Make header
        System.arraycopy(intToByteArray(sequenceNumber), 0, data, 0, 2);

        // Make payload
        byte[] payload = new byte[PACKET_SIZE - 2];
        new Random().nextBytes(payload);
        System.arraycopy(payload, 0, data, 2, PACKET_SIZE - 2);
    }

    public byte[] getData() {
        return data;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}
