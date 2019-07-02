package Receiver;

import Logger.Log;
import Packet.Packet;
import Packet.ReceiverPacket;
import Sender.Sender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class Receiver {
    public static int port;
    private int num;
    private int win;
    private int l;
    private int windowLeftIndex;
    private boolean initIsDone;

    final Thread receiverSendThread;
    final Thread receiverReceiveThread;
    final Thread receiverMoveWindowThread;

    private final Queue<ReceiverPacket> receiverPacketsQueue;
    private ReceiverPacket[] receiverPacketArray;

    private DatagramSocket datagramSocket;
    private static final int SENDER_PACKET_LENGTH = 512;
    private boolean[] bitmap;

    private Receiver() {
        receiverPacketsQueue = new LinkedList<>();
        initIsDone = false;

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

        Receiver.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
        windowLeftIndex = 0;

        initReceiver();
    }

    Receiver(int port, int num, int win, int l) throws IOException {
        this();

        Receiver.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
        windowLeftIndex = 0;

        initReceiver();
    }

    Receiver(int port, int num, int l, String logFileAddress) throws IOException {
        this();

        Receiver.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
        windowLeftIndex = 0;
        Log.createLogFile(logFileAddress);
        initReceiver();
    }

    Receiver(int port, int num, int win, int l, String logFileAddress) throws IOException {
        this();

        Receiver.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
        windowLeftIndex = 0;
        Log.createLogFile(logFileAddress);
        initReceiver();
    }

    private void receivePacket() throws IOException {
        while (initIsDone) {
            byte[] receivedPacket = new byte[SENDER_PACKET_LENGTH];
            DatagramPacket dp = new DatagramPacket(receivedPacket, SENDER_PACKET_LENGTH);
            try {
                datagramSocket.receive(dp);
            } catch (SocketTimeoutException ste) {
                // TODO check if exit is exiting from the whole app not just this thread
                Log.senderTimeoutLog(System.currentTimeMillis());
                System.exit(3);
            }
            receivedPacket = dp.getData();
            byte[] sequenceNumberBytes = {receivedPacket[0], receivedPacket[1]};
            int sequenceNumber = Packet.byteArrayToInt(sequenceNumberBytes);
            Log.receiverReceivePacketsLog(System.currentTimeMillis(), sequenceNumber);

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

    private void sendAck() throws InterruptedException, IOException {
        while (initIsDone) {
            while (receiverPacketsQueue.isEmpty()) Thread.sleep(25);
            ReceiverPacket receiverPacket = receiverPacketsQueue.poll();
            if (checkLostRate()) {
                DatagramPacket ack = new DatagramPacket(receiverPacket.getData(), receiverPacket.getData().length, InetAddress.getLocalHost(), port);
                datagramSocket.send(ack);
                int length = windowLeftIndex + win < num ? win : num - windowLeftIndex;
                boolean[] ackBitmap = new boolean[win];
                System.arraycopy(bitmap, windowLeftIndex, ackBitmap, 0, length);
                Log.receiverSendAckLog(System.currentTimeMillis(), windowLeftIndex, windowLeftIndex + length, ackBitmap);
            }
        }
    }

    private void receiverMoveWindow() {
        while (initIsDone) {
            while (bitmap[windowLeftIndex]) {
                windowLeftIndex++;
            }
        }
    }

    private byte[] makeAckBitmap() {
        int length = windowLeftIndex + win < num ? win : num - windowLeftIndex;

        boolean[] ackBitMap = new boolean[win];
        System.arraycopy(bitmap, windowLeftIndex, ackBitMap, 0, length);

        return toBytes(ackBitMap);
    }

    @Contract(pure = true)
    private byte[] toBytes(@NotNull boolean[] input) {
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

    private void initReceiver() throws SocketException {
        bitmap = new boolean[num];
        datagramSocket = new DatagramSocket(Sender.port);
        datagramSocket.setSoTimeout(1000);
        receiverPacketArray = new ReceiverPacket[num];
        initIsDone = true;
    }
}
