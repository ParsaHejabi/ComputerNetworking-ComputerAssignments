package Receiver;

import Packet.Packet;
import Packet.ReceiverPacket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

class Receiver {
    private static final String projectPath = new File("./Logs").getAbsolutePath();

    private int port;
    private int num;
    private int win;
    private int l;
    private int windowLeftIndex;

    /**
     * TODO when writing logs check if @param logFileAddress is null write to System.out
     */
    private String logFileAddress;
    private FileWriter logFileWriter;

    private DateFormat dateFormat;

    Thread receiverSendThread;
    Thread receiverReceiveThread;
    Thread receiverMoveWindowThread;

    private Queue<ReceiverPacket> receiverPacketsQueue;
    private ReceiverPacket[] receiverPacketArray;

    private DatagramSocket datagramSocket;
    private static int senderPacketLength = 512;
    private boolean[] bitmap;

    private Receiver(){
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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

        receiverMoveWindowThread = new Thread(() -> {
            try {
                receiverMoveWindow();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        });
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

        this.logFileAddress = logFileAddress;

        createLogFile(logFileAddress);
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

        this.logFileAddress = logFileAddress;

        createLogFile(logFileAddress);
    }

    private void createLogFile(String logFileAddress) throws IOException {
        int lastSlashIndex = logFileAddress.lastIndexOf("/");
        String dirs = logFileAddress.substring(0, lastSlashIndex);
        if (new File(projectPath + dirs).mkdirs()) {
            System.out.println(dateFormat.format(new Date()) + ":\tCreated log file directory.");

            this.logFileWriter = new FileWriter(projectPath + logFileAddress);
        } else {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            System.out.println(dateFormat.format(new Date()) + ":\tFailed to create log file directory. exiting application.");
            System.exit(2);
        }
    }

    private void receivePacket() throws IOException {
        datagramSocket.setSoTimeout(60000);
        while (true) {
            byte[] receivedPacket = new byte[senderPacketLength];
            DatagramPacket dp = new DatagramPacket(receivedPacket, senderPacketLength);
            try {
                datagramSocket.receive(dp);
            } catch (SocketTimeoutException ste) {
                System.exit(3);
            }
            receivedPacket = dp.getData();
            System.out.println("Message #" + receivedPacket[0] + " received.");

            byte[] sequenceNumberBytes = {receivedPacket[0], receivedPacket[1]};
            int sequenceNumber = Packet.byteArrayToInt(sequenceNumberBytes);

            if (!bitmap[sequenceNumber]) { // This is the first time we're receiving this packet
                bitmap[sequenceNumber] = true;
                boolean[] booleanMap = new boolean[win];
                System.arraycopy(bitmap, windowLeftIndex, booleanMap, 0, win);
                byte[] ackBitmap = makeAckBitmap(booleanMap);
                ReceiverPacket receiverPacket = new ReceiverPacket(win, sequenceNumber, ackBitmap);
                receiverPacketArray[sequenceNumber] = receiverPacket;
            } else { // We have already received this packet, its ack is either in the queue or it has been sent
                ReceiverPacket receiverPacket = receiverPacketArray[sequenceNumber];
                if (!receiverPacketsQueue.contains(receiverPacket)) {
                    receiverPacketsQueue.add(receiverPacket);
                }
            }
        }
    }

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

    private void receiverMoveWindow() throws InterruptedException {
        while (true) {
            while (bitmap[windowLeftIndex]) {
                windowLeftIndex++;
            }
            Thread.sleep(50);
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
