package Receiver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private Thread receiverSendThread;
    private Thread receiverReceiveThread;
    private Thread receiverMoveWindowThread;

    private Queue<ReceiverPacket> receiverPacketsQueue;

    public Receiver() {
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        receiverPacketsQueue = new LinkedList<>();

        receiverSendThread = new Thread(() -> {

        });

        receiverReceiveThread = new Thread(() -> {

        });

        receiverMoveWindowThread = new Thread(() -> {

        });
    }

    public Receiver(int port, int num, int l) {
        this();

        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
    }

    public Receiver(int port, int num, int win, int l) {
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
    }

    public Receiver(int port, int num, int l, String logFileAddress) throws IOException {
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;

        this.logFileAddress = logFileAddress;

        createLogFile(logFileAddress);
    }

    public Receiver(int port, int num, int win, int l, String logFileAddress) throws IOException {
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
}
