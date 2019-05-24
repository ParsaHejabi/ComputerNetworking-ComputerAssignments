package Receiver;

public class ReceiverPacket {
    private byte[] data;

    public ReceiverPacket(int win, byte[] sequenceNumber, byte[] bitSequence) {
        this.data = new byte[2 + (win / 8)];

        System.arraycopy(sequenceNumber, 0, data, 0, sequenceNumber.length);
        System.arraycopy(bitSequence, 0, data, 2, bitSequence.length);
    }

    public byte[] getData() {
        return data;
    }
}
