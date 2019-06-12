package Sender;

import java.io.IOException;

class SenderRunner {
    public static void main(String[] args) throws IOException, InterruptedException {
        Sender sender = createSender(args);
        sender.senderSendThread.start();
        sender.senderReceiveThread.start();
        sender.senderMoveWindowThread.start();
    }

    private static Sender createSender(String[] args) throws IOException, InterruptedException {
        Sender sender = null;

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        int num = Integer.parseInt(args[2]);

        if (args.length > 6 || args.length < 4) {
            System.exit(1);
        } else if (args.length == 4) {
            sender = new Sender(ip, port, num, Integer.parseInt(args[3]));
        } else if (args.length == 5) {
            int win;
            String logFileAddress;

            try {
                win = Integer.parseInt(args[3]);
                sender = new Sender(ip, port, num, win, Integer.parseInt(args[4]));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                logFileAddress = args[4];
                sender = new Sender(ip, port, num, Integer.parseInt(args[3]), logFileAddress);
            }
        } else {
            sender = new Sender(ip, port, num, Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
        }

        return sender;
    }
}