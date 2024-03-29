import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import org.xbill.DNS.*;

class Proxy {
    static final String PROXY_ADDRESS = "localhost";
    static final int UDP_PORT_NUMBER = 161;
    private static final int TCP_PORT_NUMBER = 80;
    private static final int PROXY_TCP_PORT_NUMBER = 53;

    private ServerSocket proxyTCPServerSocket;
    private Socket clientSocket;

    private Socket proxyTCPSocket;
    private DatagramSocket proxyUDPSocket;
    private InetAddress clientAddress;
    private int clientPort = -1;

    private byte[] proxyUDPBuffer;

    private Thread proxySendUDPThread;
    private Thread proxyReceiveUDPThread;
    private Thread proxySendTCPThread;
    private Thread proxyReceiveTCPThread;

    private Thread proxyDNSReceiveTCPThread;
    private Thread proxyDNSSendAndReceiveUDPThread;
    private Thread proxyDNSSendTCPThread;

    private Queue<String> sendTCPQueue;
    private Queue<String> sendUDPQueue;
    private Queue<String> sendDNSTCPQueue;
    private Queue<String> sendDNSUDPQueue;

    private Proxy() throws IOException {
        proxyUDPSocket = new DatagramSocket(UDP_PORT_NUMBER);
        proxyTCPServerSocket = new ServerSocket(PROXY_TCP_PORT_NUMBER);

        proxyUDPBuffer = new byte[16777216];

        sendTCPQueue = new LinkedList<>();
        sendUDPQueue = new LinkedList<>();
        sendDNSTCPQueue = new LinkedList<>();
        sendDNSUDPQueue = new LinkedList<>();

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

//        proxyDNSReceiveTCPThread = new Thread(() -> {
//            try {
//                receiveDNS_TCP();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//
//        proxyDNSSendAndReceiveUDPThread = new Thread(() -> {
//            try {
//                sendAndReceiveDNS_UDP();
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//
//        proxyDNSSendTCPThread = new Thread(() -> {
//            try {
//                sendDNS_TCP();
//            } catch (InterruptedException | IOException e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void receiveHttpGET_UDP() throws IOException {
        //TODO Change this condition!
        while (true) {
            //Receiving packet from client (UDP)
            DatagramPacket proxyUDPPacket = new DatagramPacket(proxyUDPBuffer, proxyUDPBuffer.length);
            proxyUDPSocket.receive(proxyUDPPacket);

            clientAddress = proxyUDPPacket.getAddress();
            clientPort = proxyUDPPacket.getPort();

            String received = new String(proxyUDPPacket.getData(), 0, proxyUDPPacket.getLength(), StandardCharsets.UTF_8);

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

            System.out.println(data);

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
            //System.out.println(data);

            if (data == null) {
                Thread.sleep(200);
                continue;
            }

            System.out.println(data);
            DatagramPacket proxyUDPPacket = new DatagramPacket(data.getBytes(StandardCharsets.UTF_8), data.length(), clientAddress, clientPort);
            proxyUDPSocket.send(proxyUDPPacket);
        }
    }

    private void receiveDNS_TCP() throws IOException {
        //TODO Change this condition!
        while (true) {
            //Receiving DNS packet from client (TCP)
            clientSocket = proxyTCPServerSocket.accept();
            System.out.println("Client connected to send DNS packet via TCP.");

            InputStream DNSInputStream = clientSocket.getInputStream();
            byte[] received = new byte[4096];
            int count = DNSInputStream.read(received);

            //DEBUGGING PURPOSE
            System.out.println("DNS TCP Packet received from client.");
            System.out.println("Number of bytes read: " + count);

            String out = new String(received, 0, count);
//            System.out.println(out);
            sendDNSUDPQueue.add(out);
        }
    }

    private void sendAndReceiveDNS_UDP() throws IOException, InterruptedException {
        //TODO Change this condition!
        while (true) {
            if (sendDNSUDPQueue.isEmpty()) {
                Thread.sleep(200);
                continue;
            }

            String data = sendDNSUDPQueue.poll();

            if (data.startsWith("a")){
                InetAddress addr = Address.getByName(data.split("\n")[1]);
                System.out.println("DNS answer received from DNS server:");
                System.out.println(addr.getHostAddress());
                sendDNSTCPQueue.add(addr.getHostAddress());
            }
            else if (data.startsWith("c")){
                InetAddress addr = Address.getByName(data.split("\n")[1]);
                System.out.println("DNS answer received from DNS server:");
                System.out.println(addr.getCanonicalHostName());
                sendDNSTCPQueue.add(addr.getCanonicalHostName());
            }
        }
    }

    private void sendDNS_TCP() throws InterruptedException, IOException {
        while (true) {
            if (sendDNSTCPQueue.isEmpty()) {
                Thread.sleep(200);
                continue;
            }

            String data = sendDNSTCPQueue.poll();

            OutputStream DNSOutputStream = clientSocket.getOutputStream();
            DNSOutputStream.write(data.getBytes());
        }
    }

    private void close() throws IOException {
        proxyTCPSocket.close();
        proxyUDPSocket.close();
    }

    public static void main(String[] args) throws IOException {
        Proxy proxy1 = new Proxy();
        proxy1.proxyReceiveUDPThread.start();
        proxy1.proxySendTCPThread.start();
        proxy1.proxyReceiveTCPThread.start();
        proxy1.proxySendUDPThread.start();

//        proxy1.proxyDNSReceiveTCPThread.start();
//        proxy1.proxyDNSSendAndReceiveUDPThread.start();
//        proxy1.proxyDNSSendTCPThread.start();
    }
}
