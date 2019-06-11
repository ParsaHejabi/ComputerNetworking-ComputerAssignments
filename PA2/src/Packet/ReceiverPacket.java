package Packet;

public class ReceiverPacket extends Packet {
    private byte[] data;

    public ReceiverPacket(int win, int sequenceNumber, byte[] bitSequence) {
        this.data = new byte[2 + (win / 8)];

        // Make header
        System.arraycopy(intToByteArray(sequenceNumber), 0, data, 0, 2);

        System.arraycopy(bitSequence, 0, data, 2, bitSequence.length);
    }

    public byte[] getData() {
        return data;
    }
}
