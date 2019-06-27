package Receiver;

import Logger.Log;
import Packet.Packet;
import Packet.ReceiverPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;

class Receiver {
    private int port;
    private int num;
    private int win;
    private int l;
    private int windowLeftIndex;

    Thread receiverSendThread;
    Thread receiverReceiveThread;
    Thread receiverMoveWindowThread;

    private Queue<ReceiverPacket> receiverPacketsQueue;
    private ReceiverPacket[] receiverPacketArray;

    private DatagramSocket datagramSocket;
    private static final int SENDER_PACKET_LENGTH = 512;
    private boolean[] bitmap;

    private Receiver() {
        receiverPacketsQueue = new LinkedList<>();

        receiverSendThread = new Thread(() -> {
            try {
                sendAck();
            } catch (InterruptedException | IOException ie) {
                ie.printStackTrace();
            }
        });

        receiverReceiveThread = new Thread(() -> {
            try {
                receivePacket();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });

        receiverMoveWindowThread = new Thread(this::receiverMoveWindow);
    }

    Receiver(int port, int num, int l) throws IOException {
        this();

        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
        windowLeftIndex = 0;

        bitmap = new boolean[num];
        datagramSocket = new DatagramSocket(port);
        receiverPacketArray = new ReceiverPacket[num];
    }

    Receiver(int port, int num, int win, int l) throws IOException {
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
        windowLeftIndex = 0;

        bitmap = new boolean[num];
        datagramSocket = new DatagramSocket(port);
        receiverPacketArray = new ReceiverPacket[num];
    }

    Receiver(int port, int num, int l, String logFileAddress) throws IOException {
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
        windowLeftIndex = 0;

        bitmap = new boolean[num];
        datagramSocket = new DatagramSocket(port);
        receiverPacketArray = new ReceiverPacket[num];

        Log.createLogFile(logFileAddress);
    }

    Receiver(int port, int num, int win, int l, String logFileAddress) throws IOException {
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
        windowLeftIndex = 0;

        bitmap = new boolean[num];
        datagramSocket = new DatagramSocket(port);
        receiverPacketArray = new ReceiverPacket[num];

        Log.createLogFile(logFileAddress);
    }

    private void receivePacket() throws IOException {
        datagramSocket.setSoTimeout(1000);
        while (true) {
            byte[] receivedPacket = new byte[SENDER_PACKET_LENGTH];
            DatagramPacket dp = new DatagramPacket(receivedPacket, SENDER_PACKET_LENGTH);
            try {
                datagramSocket.receive(dp);
            } catch (SocketTimeoutException ste) {
                // TODO log SENDER TIMEOUT and close the log
                System.exit(3);
            }
            receivedPacket = dp.getData();
            byte[] sequenceNumberBytes = {receivedPacket[0], receivedPacket[1]};
            int sequenceNumber = Packet.byteArrayToInt(sequenceNumberBytes);

            if (!bitmap[sequenceNumber]) { // This is the first time we're receiving this packet
                bitmap[sequenceNumber] = true;
                boolean[] booleanMap = new boolean[win];
                // TODO check if the window size is less than win
                System.arraycopy(bitmap, windowLeftIndex, booleanMap, 0, win);
                byte[] ackBitmap = makeAckBitmap(booleanMap);
                ReceiverPacket receiverPacket = new ReceiverPacket(win, sequenceNumber, ackBitmap);
                receiverPacketArray[sequenceNumber] = receiverPacket;
                receiverPacketsQueue.add(receiverPacket);
            } else { // We have already received this packet, its ack is either in the queue or it has been sent
                ReceiverPacket receiverPacket = receiverPacketArray[sequenceNumber];
                if (!receiverPacketsQueue.contains(receiverPacket)) {
                    receiverPacketsQueue.add(receiverPacket);
                }
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void sendAck() throws InterruptedException, IOException {
        while (true) {
            while (receiverPacketsQueue.isEmpty()) Thread.sleep(50);
            ReceiverPacket receiverPacket = receiverPacketsQueue.poll();
            if (checkLostRate()) {
                DatagramPacket ack = new DatagramPacket(receiverPacket.getData(), receiverPacket.getData().length, port);
                datagramSocket.send(ack);
            }
        }
    }

    private void receiverMoveWindow() {
        while (true) {
            while (bitmap[windowLeftIndex]) {
                windowLeftIndex++;
            }
        }
    }

    private byte[] makeAckBitmap(boolean[] booleanMap) {
        int intMap = 0;
        for (int i = 0; i < win; i++) {
            intMap = (intMap << 1) + ((booleanMap[i] ? 1 : 0) & 0x01);
        }
        return Packet.intToByteArray(intMap);
    }

    private boolean checkLostRate() {
        return (Math.random() * 100) >= this.l;
    }
}
