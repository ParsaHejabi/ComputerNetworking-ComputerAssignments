package Sender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class Sender {
    private static final String projectPath = new File("./Logs").getAbsolutePath();

    private String ip;
    private int port;
    private int num;
    private int win;
    private int l;

    /**
     * TODO when writing logs check if @param logFileAddress is null write to System.out
     */
    private String logFileAddress;
    private FileWriter logFileWriter;

    private DateFormat dateFormat;
    private int[] bitmap;
    private int leftIndex;

    Thread senderSendThread;
    Thread senderReceiveThread;
    Thread senderMoveWindowThread;

    private Queue<SenderPacket> packetsQueue;

    private DatagramSocket datagramSocket;

    Sender() throws IOException {
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        packetsQueue = new LinkedList<>();
        datagramSocket = new DatagramSocket();
        leftIndex = 0;

        senderSendThread = new Thread(() -> {
            try {
                sendMessage();
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
            } catch (InterruptedException ie) {
                ie.printStackTrace();
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

        for (int i = 0; i < win; i++) {
            packetsQueue.add(new SenderPacket((byte) i, (byte) num));
        }
        bitmap = new int[num];
    }

    Sender(String ip, int port, int num, int win, int l) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
    }

    Sender(String ip, int port, int num, int l, String logFileAddress) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;

        this.logFileAddress = logFileAddress;
        createLogFile(logFileAddress);
    }

    Sender(String ip, int port, int num, int win, int l, String logFileAddress) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;

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

    private void sendMessage() throws InterruptedException, UnknownHostException, IOException {
        /***
         * TODO change sendMessage to send packets according to current window (it only send the first 128 packets now)
         */
        while (true) {
            while (packetsQueue.isEmpty()) Thread.sleep(50);
            SenderPacket sp = packetsQueue.poll();
            DatagramPacket message = new DatagramPacket(sp.getData(), sp.getData().length, InetAddress.getByName(ip), port);
            datagramSocket.send(message);
            System.out.println("Message #" + sp.getData()[0] + " sent.");

        }
    }

    private void receiveAck() throws IOException {
        byte[] ack = new byte[2 + win / 8];
        DatagramPacket dp = new DatagramPacket(ack, ack.length);
        datagramSocket.receive(dp);
        System.out.print("Ack received: ");
        int leftAckIndex = (int) ack[0];
        for (int i = 0; i < win / 8; i++) {
            String s = String.format("%8s", Integer.toBinaryString(ack[i + 2] & 0xFF));
            for (int j = 0; j < 8; j++) {
                bitmap[leftAckIndex + i * 8 + j] = s.charAt(j) - 48;
                System.out.print(bitmap[leftAckIndex + i * 8 + j]);
            }
        }
        System.out.println();
    }

    private void senderMoveWindow() throws InterruptedException {
        while (true) {
            if (bitmap[leftIndex] == 1) {
                leftIndex++;
                System.out.println("Window move: 1\tCurrent window start index: " + leftIndex);
            } else {
                int ones = 0;
                int remained = num - leftIndex > win ? win : num - leftIndex;
                //COUNTING ONES IN CURRENT WINDOW
                for (int i = leftIndex; i < leftIndex + remained; i++) {
                    ones += bitmap[i];
                }
                //CHECKING IF THE WINDOW CAN BE MOVED
                if (((remained - ones) / remained) * 100 < l) {
                    leftIndex += remained;
                    System.out.println("Window move: " + remained + "\tCurrent window start index: " + leftIndex);
                    if (leftIndex == num - 1) {//END OF PROGRAM

                    }
                }
            }
            Thread.sleep(75);
        }
    }
}
