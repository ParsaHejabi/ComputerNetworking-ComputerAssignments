package Receiver;

import java.io.IOException;

class ReceiverRunner {

    public static void main(String[] args) throws IOException {
        Receiver receiver = createReceiver(args);
        receiver.receiverReceiveThread.start();
        receiver.receiverMoveWindowThread.start();
        receiver.receiverSendThread.start();
    }

    private static Receiver createReceiver(String[] args) throws IOException {
        Receiver receiver = null;

        int port = Integer.parseInt(args[0]);
        int num = Integer.parseInt(args[1]);

        if (args.length > 5 || args.length < 3) {
            System.exit(1);
        } else if (args.length == 3) {
            receiver = new Receiver(port, num, Integer.parseInt(args[2]));
        } else if (args.length == 4) {
            int win;
            String logFileAddress;

            try {
                win = Integer.parseInt(args[2]);
                receiver = new Receiver(port, num, win, Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                logFileAddress = args[3];
                receiver = new Receiver(port, num, Integer.parseInt(args[2]), logFileAddress);
            }
        } else {
            receiver = new Receiver(port, num, Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4]);
        }

        return receiver;
    }
}
