package Sender;

import Logger.Log;
import Packet.Packet;
import Packet.SenderPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class Sender {
    private String ip;
    private int port;
    private int num;
    private int win;
    private int l;

    Thread senderSendThread;
    Thread senderReceiveThread;
    Thread senderMoveWindowThread;

    private ArrayList<SenderPacket> senderPackets;
    private final Queue<SenderPacket> sendingQueue;
    private ArrayList<Long> startTimes;

    //-1 when ack is received in Sender otherwise number of times it has been sent
    private int[] senderBitmap;
    private int windowLeftIndex;

    private DatagramSocket datagramSocket;

    private boolean initIsDone = false;

    private Sender() throws IOException {
        sendingQueue = new LinkedList<>();
        datagramSocket = new DatagramSocket(120);
        windowLeftIndex = 0;

        senderSendThread = new Thread(() -> {
            try {
                sendPacket();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        senderReceiveThread = new Thread(() -> {
            try {
                receiveAck();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });

        senderMoveWindowThread = new Thread(() -> {
            try {
                senderMoveWindow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    Sender(String ip, int port, int num, int l) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;

        initSenderPackets();
    }

    Sender(String ip, int port, int num, int win, int l) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;

        senderBitmap = new int[num];

        initSenderPackets();
    }

    Sender(String ip, int port, int num, int l, String logFileAddress) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;

        Log.createLogFile(logFileAddress);

        senderBitmap = new int[num];

        initSenderPackets();
    }

    Sender(String ip, int port, int num, int win, int l, String logFileAddress) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;

        Log.createLogFile(logFileAddress);

        senderBitmap = new int[num];

        initSenderPackets();
    }

    /**
     * @throws InterruptedException if any thread has interrupted the current thread. The
     *                              <i>interrupted status</i> of the current thread is
     *                              cleared when this exception is thrown.
     * @throws IOException          if an I/O error occurs.
     */
    private void sendPacket() throws InterruptedException, IOException {
        while (initIsDone) {
            while (sendingQueue.isEmpty()) Thread.sleep(5);
            SenderPacket packetToSend = sendingQueue.poll();
            int sequenceNumber = packetToSend.getSequenceNumber();
            senderBitmap[sequenceNumber]++;
            startTimes.set(sequenceNumber, System.currentTimeMillis());

            if (checkLostRate()) {
                DatagramPacket message = new DatagramPacket(packetToSend.getData(), packetToSend.getData().length, InetAddress.getByName(ip), port);
                datagramSocket.send(message);
                Log.senderSendPacketsLog(System.currentTimeMillis(), sequenceNumber, senderBitmap[sequenceNumber] - 1 == 0 ? "TX" : "RTX");
            }
        }
    }

    /**
     * @return true if we have to send otherwise false
     */
    private boolean checkLostRate() {
        return (Math.random() * 100) >= this.l;
    }

    /**
     * @throws IOException TODO add code for logging
     */
    private void receiveAck() throws IOException {
        while (initIsDone) {
            byte[] ack = new byte[2 + (win / 8)];
            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
            datagramSocket.receive(ackPacket);
            byte[] ackData = ackPacket.getData();
            byte[] ackSequenceNumberBytes = new byte[2];
            System.arraycopy(ackData, 0, ackSequenceNumberBytes, 0, 2);
            int sequenceNumber = Packet.byteArrayToInt(ackSequenceNumberBytes);
            System.out.println("Ack #" + sequenceNumber + " received.");
            if (senderBitmap[sequenceNumber] != -1)
                senderBitmap[sequenceNumber] = -1;
        }
    }

    private void senderMoveWindow() throws IOException {
        int losses = 0;
        while (initIsDone) {
            while (senderBitmap[windowLeftIndex] == -1) {
                long time = System.currentTimeMillis() - startTimes.get(windowLeftIndex);
                int oldStart = windowLeftIndex;
                windowLeftIndex++;
                if (windowLeftIndex == num) {
                    System.out.println("All Packets successfully sent.\nAll Acks successfully received.");
                    System.exit(3);
                }
                int newStart = windowLeftIndex;
                int lastIndexOfWindow = (windowLeftIndex + this.win < num) ? (windowLeftIndex + this.win) : (num - 1);
                int[] logBitmap = new int[lastIndexOfWindow - windowLeftIndex + 1];
                for (int i = windowLeftIndex; i <= lastIndexOfWindow; i++) {
                    logBitmap[i - windowLeftIndex] = (senderBitmap[i] == -1) ? 1 : 0;
                }
                Log.senderShiftWindowLog(time, oldStart, newStart, logBitmap, losses);

            }

            losses = 0;

            int lastIndexOfWindow = (windowLeftIndex + this.win < num) ? (windowLeftIndex + this.win) : (num - 1);
            for (int i = windowLeftIndex; i <= lastIndexOfWindow; i++) {
                if (senderBitmap[i] != -1) {
                    if (senderBitmap[i] == 0) {
                        if (!sendingQueue.contains(senderPackets.get(i))) {
                            sendingQueue.add(senderPackets.get(i));
                        }
                    } else {
                        losses++;
                        if (!sendingQueue.contains(senderPackets.get(i))) {
                            // Timeout
                            if (System.currentTimeMillis() - startTimes.get(i) > 100) {
                                if (senderBitmap[i] == 8) {
                                    // TODO check if exit is exiting from the whole app not just this thread
                                    Log.receiverTimeoutLog(System.currentTimeMillis());
                                    System.exit(3);
                                }
                                sendingQueue.add(senderPackets.get(i));
                            }
                            // Wait till timeout or ack
                        }
                    }
                }
            }
        }
    }

    private void initSenderPackets() {
        senderBitmap = new int[num];
        startTimes = new ArrayList<>(num);
        senderPackets = new ArrayList<SenderPacket>(num);
        for (int i = 0; i < num; i++) {
            startTimes.add(0L);
            senderPackets.add(new SenderPacket(i));
        }
        initIsDone = true;
    }
}
