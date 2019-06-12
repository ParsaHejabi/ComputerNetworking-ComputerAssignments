package Packet;

public class Packet {

    public static byte[] intToByteArray(int number) {
        byte[] result = new byte[2];
        result[0] = (byte) (number & 0xFF);
        result[1] = (byte) ((number >> 8) & 0xFF);
        return result;
    }

    public static int byteArrayToInt(byte[] data) {
        return ((data[1] & 0xFF) << 8) |
                (data[0] & 0xFF);
    }
}
