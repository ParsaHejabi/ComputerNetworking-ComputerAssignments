package Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Log {
    private static final String projectPath = new File("./Logs").getAbsolutePath();

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static FileWriter logFileWriter;
    private static boolean hasLogFile;

    public static void createLogFile(@NotNull String logFileAddress) throws IOException {
        int lastSlashIndex = logFileAddress.lastIndexOf("/");
        String dirs = logFileAddress.substring(0, lastSlashIndex);
        if (new File(projectPath + dirs).mkdirs()) {
            System.out.println(dateFormat.format(new Date()) + ":\tCreated log file directory.");

            logFileWriter = new FileWriter(projectPath + logFileAddress);
            hasLogFile = true;
        } else {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            System.out.println(dateFormat.format(new Date()) + ":\tFailed to create log file directory. exiting application.");
            System.exit(2);
        }
    }

    public static void senderSendPacketsLog(long time, int sequenceNumber, String event) throws IOException {
        String logMessage = dateFormat.format(new Date(time)) + ", " + sequenceNumber + ", " + event;
        if (hasLogFile) {
            logFileWriter.write(logMessage);
            logFileWriter.flush();
        } else {
            System.out.println(logMessage);
        }
    }

    public static void senderShiftWindowLog(
            long time,
            int oldStart,
            int newStart,
            @NotNull int[] senderWindowBitmap,
            int losses
    ) throws IOException {
        char[] bitmapToShow = new char[senderWindowBitmap.length];
        for (int i = 0; i < senderWindowBitmap.length; i++) {
            if (senderWindowBitmap[i] == 0) {
                bitmapToShow[i] = '0';
            } else {
                bitmapToShow[i] = '1';
            }
        }
        String logMessage = dateFormat.format(new Date(time)) + ", "
                + oldStart + ", "
                + newStart + ", "
                + Arrays.toString(bitmapToShow) + ", "
                + losses;
        if (hasLogFile) {
            logFileWriter.write(logMessage);
            logFileWriter.flush();
        } else {
            System.out.println(logMessage);
        }
    }

    public static void receiverSendAckLog(long time, int start, int end, boolean[] bitmap) throws IOException {
        String logMessage = dateFormat.format(new Date(time)) + ", " + start + ":" + end + Arrays.toString(bitmap);
        if (hasLogFile) {
            logFileWriter.write(logMessage);
            logFileWriter.flush();
        } else {
            System.out.println(logMessage);
        }
    }

    public static void receiverReceivePacketsLog(long time, int sequenceNumber) throws IOException {
        String logMessage = dateFormat.format(new Date(time)) + ", " + sequenceNumber + ", " + "RX";
        if (hasLogFile) {
            logFileWriter.write(logMessage);
            logFileWriter.flush();
        } else {
            System.out.println(logMessage);
        }
    }

    public static void senderTimeoutLog(long time) throws IOException {
        String logMessage = dateFormat.format(new Date(time)) + " SENDER TIMEOUT";
        if (hasLogFile) {
            logFileWriter.write(logMessage);
            logFileWriter.flush();
            logFileWriter.close();
        } else {
            System.out.println(logMessage);
        }
    }

    public static void receiverTimeoutLog(long time) throws IOException {
        String logMessage = dateFormat.format(new Date(time)) + " RECEIVER TIMEOUT";
        if (hasLogFile) {
            logFileWriter.write(logMessage);
            logFileWriter.flush();
            logFileWriter.close();
        } else {
            System.out.println(logMessage);
        }
    }
}
