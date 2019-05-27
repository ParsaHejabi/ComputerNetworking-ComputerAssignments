package Sender;

import java.util.Random;

public class SenderPacket {
    private static final int PACKET_SIZE = 512;
    private byte[] data;

    public SenderPacket(byte sequenceNumber, byte numberOfPackets) {
        this.data = new byte[PACKET_SIZE];

        makeHeader(this.data, sequenceNumber, numberOfPackets);
        makePayload(this.data);
    }

    private void makePayload(byte[] data) {
        byte[] payload = new byte[510];
        new Random().nextBytes(payload);
        System.arraycopy(payload, 0, data, 2, payload.length);
    }

    //TODO complete this method
    private void makeHeader(byte[] data, byte sequenceNumber, byte numberOfPackets) {
        data[0] = sequenceNumber;
        data[1] = numberOfPackets;
    }

    public byte[] getData() {
        return data;
    }
}
