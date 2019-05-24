package Sender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private Thread senderSendThread;
    private Thread senderReceiveThread;
    private Thread senderMoveWindowThread;

    private Queue<SenderPacket> packetsQueue;

    public Sender() {
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        packetsQueue = new LinkedList<>();

        senderSendThread = new Thread(() -> {

        });

        senderReceiveThread = new Thread(() -> {

        });

        senderMoveWindowThread = new Thread(() -> {

        });
    }

    public Sender(String ip, int port, int num, int l) {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;
    }

    public Sender(String ip, int port, int num, int win, int l) {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = win;
        this.l = l;
    }

    public Sender(String ip, int port, int num, int l, String logFileAddress) throws IOException {
        this();

        this.ip = ip;
        this.port = port;
        this.num = num;
        this.win = 128;
        this.l = l;

        this.logFileAddress = logFileAddress;
        createLogFile(logFileAddress);
    }

    public Sender(String ip, int port, int num, int win, int l, String logFileAddress) throws IOException {
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
}
