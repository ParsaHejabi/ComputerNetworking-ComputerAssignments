package Receiver;

import Packet.ReceiverPacket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class Receiver {
    private static final String projectPath = new File("./Logs").getAbsolutePath();

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

    Thread receiverSendThread;
    Thread receiverReceiveThread;
    Thread receiverMoveWindowThread;

    private Queue<ReceiverPacket> receiverPacketsQueue;

    private DatagramSocket datagramSocket;
    private static int senderPacketLength = 512;
    private int[] bitmap;
    private boolean checkWindow;

    Receiver() throws IOException {
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        receiverPacketsQueue = new LinkedList<>();
        checkWindow = false;

        receiverSendThread = new Thread(() -> {
            sendAck();
        });

        receiverReceiveThread = new Thread(() -> {
            try {
                receiveMessage();
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

    /***
     * TODO DatagramSocket and bitmap must be instantiated in each constructor (bc we need win and port)
     * ***/
    Receiver(int port, int num, int l) throws IOException {
        this();

        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
        bitmap = new int[num];
        datagramSocket = new DatagramSocket(port);
    }

    Receiver(int port, int num, int win, int l) {
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
    }

    Receiver(int port, int num, int l, String logFileAddress) throws IOException {
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;

        this.logFileAddress = logFileAddress;

        createLogFile(logFileAddress);
    }

    Receiver(int port, int num, int win, int l, String logFileAddress) throws IOException {
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

    private void receiveMessage() throws IOException {
        while (true) {
            byte[] receivedPacket = new byte[senderPacketLength];
            DatagramPacket dp = new DatagramPacket(receivedPacket, senderPacketLength);
            datagramSocket.receive(dp);
            receivedPacket = dp.getData();
            System.out.println("Message #" + receivedPacket[0] + " received.");
            bitmap[receivedPacket[0]] = 1;
        }
    }

    private void sendAck() {
        /***
         * TODO Complete this method
         */
        while (true) {

        }
    }

    private void receiverMoveWindow() throws InterruptedException {
        /***
         * TODO check to see if we really need to have this window in Receiver
         */
        int leftIndex = 0;
        while (true) {
            if (bitmap[leftIndex] == 1) {
                leftIndex++;
                System.out.println("Window move: 1");
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
                    System.out.println("Window move: " + remained);
                    if (leftIndex == num - 1) {//END OF PROGRAM

                    }
                }
            }
            Thread.sleep(50);
        }
    }
}
