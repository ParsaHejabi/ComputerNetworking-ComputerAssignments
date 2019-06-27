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
                byte[] ackBitmap = makeAckBitmap();
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

    private byte[] makeAckBitmap() {
        int length = windowLeftIndex + win < num ? win : num - windowLeftIndex;

        boolean[] ackBitMap = new boolean[win];
        System.arraycopy(bitmap,windowLeftIndex,ackBitMap,0,length);

        return toBytes(ackBitMap);
    }

    private byte[] toBytes(boolean[] input) {
        byte[] toReturn = new byte[input.length / 8];
        for (int entry = 0; entry < toReturn.length; entry++) {
            for (int bit = 0; bit < 8; bit++) {
                if (input[entry * 8 + bit]) {
                    toReturn[entry] |= (128 >> bit);
                }
            }
        }
        return toReturn;
    }

    private boolean checkLostRate() {
        return (Math.random() * 100) >= this.l;
    }
}
