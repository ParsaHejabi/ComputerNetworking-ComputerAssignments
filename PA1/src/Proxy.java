import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class Proxy {

    static final String PROXY_ADDRESS = "localhost";
    static final int UDP_PORT_NUMBER = 161;
    private static final int TCP_PORT_NUMBER = 80;

    private Socket proxyTCPSocket;
    private DatagramSocket proxyUDPSocket;
    private InetAddress clientAddress;
    private int clientPort = -1;

    private byte[] proxyUDPBuffer;

    private Thread proxySendUDPThread;
    private Thread proxyReceiveUDPThread;
    private Thread proxySendTCPThread;
    private Thread proxyReceiveTCPThread;

    private Queue<String> sendTCPQueue;
    private Queue<String> sendUDPQueue;

    public Proxy() throws SocketException {
        proxyUDPSocket = new DatagramSocket(UDP_PORT_NUMBER);

        proxyUDPBuffer = new byte[4194304];

        sendTCPQueue = new LinkedList<>();
        sendUDPQueue = new LinkedList<>();

        proxyReceiveUDPThread = new Thread(() -> {
            try {
                receiveHttpGET_UDP();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        proxySendTCPThread = new Thread(() -> {
            try {
                sendHttpGET_TCP();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        proxyReceiveTCPThread = new Thread(() -> {
            try {
                receiveHttpGET_TCP();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        proxySendUDPThread = new Thread(() -> {
            try {
                sendHttpGET_UDP();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void receiveHttpGET_UDP() throws IOException {
        //TODO Change this condition!
        while (true) {
            //Receiving packet from client (UDP)
            DatagramPacket proxyUDPPacket = new DatagramPacket(proxyUDPBuffer, proxyUDPBuffer.length);
            proxyUDPSocket.receive(proxyUDPPacket);

            clientAddress = proxyUDPPacket.getAddress();
            clientPort = proxyUDPPacket.getPort();

            String received = new String(proxyUDPPacket.getData(), 0, proxyUDPPacket.getLength());

            //DEBUGGING PURPOSE
            System.out.println("UDP Packet received from client.");
            //System.out.println(received);

            sendTCPQueue.add(received);
        }
    }

    private void sendHttpGET_TCP() throws InterruptedException, IOException {
        //TODO Change this condition!
        while (true) {
            if (sendTCPQueue.isEmpty()) {
                Thread.sleep(200);
                continue;
            }

            String data = sendTCPQueue.poll();

            int hostNameBeginInd = data.indexOf("Host: ") + 6;
            int hostNameEndInd = data.indexOf("Accept: text/html") - 1;
            String urlHost = data.substring(hostNameBeginInd, hostNameEndInd);

            proxyTCPSocket = new Socket(urlHost, TCP_PORT_NUMBER);

            OutputStream outputStream = proxyTCPSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream);

            writer.println(data);
            writer.flush();
        }
    }

    private void receiveHttpGET_TCP() throws IOException, InterruptedException {
        while (true) {
            if (proxyTCPSocket == null) {
                Thread.sleep(200);
                continue;
            }
            InputStream inputStream = proxyTCPSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String inputLine;
            StringBuilder stringBuilder = new StringBuilder();

            while ((inputLine = reader.readLine()) != null) {
                stringBuilder.append(inputLine).append("\n");
            }

            if (stringBuilder.toString().isEmpty()){
                Thread.sleep(200);
                continue;
            }

            //System.out.println(stringBuilder.toString());
            sendUDPQueue.add(stringBuilder.toString());
        }
    }

    private void sendHttpGET_UDP() throws InterruptedException, IOException {
        while (true) {
            if (sendUDPQueue.isEmpty() || clientAddress == null || clientPort == -1) {
                Thread.sleep(200);
                continue;
            }

            String data = sendUDPQueue.poll();
            System.out.println(data);

            if (data == null) {
                Thread.sleep(200);
                continue;
            }

            DatagramPacket proxyUDPPacket = new DatagramPacket(data.getBytes(), data.length(), clientAddress, clientPort);
            proxyUDPSocket.send(proxyUDPPacket);
        }
    }

    private void close() throws IOException {
        proxyTCPSocket.close();
        proxyUDPSocket.close();
    }

    public static void main(String[] args) throws SocketException {
        Proxy proxy1 = new Proxy();
        proxy1.proxyReceiveUDPThread.start();
        proxy1.proxySendTCPThread.start();
        proxy1.proxyReceiveTCPThread.start();
        proxy1.proxySendUDPThread.start();
    }
}
