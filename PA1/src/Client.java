import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class Client {
    private static final String projectPath = new File("").getAbsolutePath();
    private static final String HTMLFilesFolder = projectPath + "/HTMLFiles/";
    private static final String HTMLExtension = ".html";

    private DatagramSocket clientUDPSocket;
    private Socket clientTCPSocket;
    private InetAddress proxyAddress;
    private static final int PROXY_PORT_NUMBER_TCP = 53;

    private URL destinationURL;

    private byte[] clientUDPBuffer;

    private Thread clientSendUDPThread;
    private Thread clientReceiveUDPThread;
    private Thread clientSendTCPThread;
    private Thread clientReceiveTCPThread;

    private BufferedWriter clientBufferedWriter;
    private Scanner scanner;

    private Queue<String> sendUDPQueue;

    private String DNSDomain;

    Client() throws IOException {
        clientUDPSocket = new DatagramSocket();
        proxyAddress = InetAddress.getByName(Proxy.PROXY_ADDRESS);
        sendUDPQueue = new LinkedList<>();

        clientUDPBuffer = new byte[16777216];

        scanner = new Scanner(System.in);

//        clientSendUDPThread = new Thread(() -> {
//            try {
//                this.sendHttpGET_UDP();
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//
//        clientReceiveUDPThread = new Thread(() -> {
//            try {
//                receiveHttpGET_UDP();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });

        clientSendTCPThread = new Thread(() -> {
            try {
                sendDNSmsg_TCP();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        clientReceiveTCPThread = new Thread(() -> {
            try {
                receiveDNSmsg_TCP();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendHttpGET_UDP() throws IOException, InterruptedException {
        //TODO Change this condition!
        while (true) {
            if (!sendUDPQueue.isEmpty()) {
                String s = sendUDPQueue.poll();
                this.destinationURL = new URL(s);
            } else {
                System.out.println("Enter host name to send HTTP UDP packet to proxy: ");
                String s = scanner.next();
                if (s.equals("END")) {
                    close();
                    break;
                }
                this.destinationURL = new URL(s);
            }

            boolean isCached = false;
            File folder = new File(HTMLFilesFolder);
            File[] listOfFiles = folder.listFiles();
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    if (listOfFile.getName().equals(destinationURL.getHost() + HTMLExtension)) {
                        System.out.println("The webpage is cached already!");
                        isCached = true;
                    }
                }
            }

            if (isCached) {
                Thread.sleep(2000);
            }
            else {
                String httpReq = "GET " + destinationURL.getPath() + " HTTP/1.1\n";
                httpReq += "Host: " + destinationURL.getHost() + "\n";
                httpReq += "Accept: text/html\n";
                httpReq += "Connection: close\n";

                //sending http udp request
                byte[] clientUDPBuffer = httpReq.getBytes(StandardCharsets.UTF_8);
                DatagramPacket clientUDPPacket = new DatagramPacket(clientUDPBuffer, clientUDPBuffer.length, proxyAddress, Proxy.UDP_PORT_NUMBER);
                clientUDPSocket.send(clientUDPPacket);
                Thread.sleep(2000);
            }
        }
    }

    private void receiveHttpGET_UDP() throws IOException {
        //TODO Change this condition!
        while (true) {
            //receiving http udp response
            DatagramPacket clientUDPPacket = new DatagramPacket(clientUDPBuffer, clientUDPBuffer.length);
            clientUDPSocket.receive(clientUDPPacket);
            String received = new String(clientUDPPacket.getData(), 0, clientUDPPacket.getLength(), StandardCharsets.UTF_8);

            //DEBUGGING PURPOSE
            System.out.println("UDP Packet received from proxy.");
            //System.out.println(received);

            String statusCode = received.substring(9, 12);

            switch (statusCode) {
                case "301":
                case "302":
                    int lastInd = received.indexOf("\n", 9);
                    System.out.println(received.substring(9, lastInd));
                    System.out.println("Redirecting...");
                    int index1 = received.indexOf("Location: ") + 10;
                    int index2 = received.indexOf("\n", index1);
                    String newLocation = received.substring(index1, index2);

                    sendUDPQueue.add(newLocation);
                    break;
                case "404":
                    System.out.println("404 Not Found. Try again!");
                    break;
                case "200":
                    int responseBodyBegin = received.indexOf("\n\n") + 2;

                    clientBufferedWriter = new BufferedWriter(new FileWriter(HTMLFilesFolder + this.destinationURL.getHost() + HTMLExtension));
                    clientBufferedWriter.write(received.substring(responseBodyBegin));
                    clientBufferedWriter.flush();
                    break;
            }
        }
    }

    private void sendDNSmsg_TCP() throws IOException, InterruptedException {
        //TODO Change this condition!
        while (true) {
            System.out.println("Please enter domain name:");
            Scanner input = new Scanner(System.in);
            DNSDomain = input.next();

            boolean isCached = false;
            HashMap<String,String> strings = getAllDNSRequestsHostName();
            for (Map.Entry<String, String> entry : strings.entrySet()) {
                if (DNSDomain.equals(entry.getKey())) {
                    System.out.println("DNS query cached already!");
                    isCached = true;
                    break;
                }
            }

            if (isCached) {
                Thread.sleep(2000);
            }
            else {
                System.out.println("A(1) or CName(2)?");
                int ac = input.nextInt();
                clientTCPSocket = new Socket(proxyAddress, PROXY_PORT_NUMBER_TCP);

                OutputStream outputStream = clientTCPSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream);

                if (ac == 1)
                    writer.println("a");
                else
                    writer.println("c");

                writer.print(DNSDomain);
                writer.flush();
                Thread.sleep(2000);
            }
        }
    }

    private HashMap<String,String> getAllDNSRequestsHostName() throws IOException {
        HashMap<String,String> answer = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(HTMLFilesFolder + "DNS.txt"));
        String line = reader.readLine();
        while (line != null) {
            String[] lines = line.split("#");
            answer.put(lines[0],lines[1]);
            line = reader.readLine();
        }
        reader.close();
        return answer;
    }

    private void receiveDNSmsg_TCP() throws IOException, InterruptedException {
        //TODO Change this condition!
        while (true) {
            if (clientTCPSocket == null) {
                Thread.sleep(200);
                continue;
            }
            InputStream input = clientTCPSocket.getInputStream();
            byte[] received = new byte[4096];
            int count = input.read(received);
            System.out.println("DNS TCP Packet received from proxy.");
            System.out.println("Number of bytes read: " + count);
            String out = new String(received, 0, count);
            clientBufferedWriter = new BufferedWriter(new FileWriter(HTMLFilesFolder + "DNS.txt", true));
            clientBufferedWriter.write(DNSDomain + "#" + out + "\n");
            clientBufferedWriter.flush();
        }
    }

    private void close() throws IOException {
        clientUDPSocket.close();
        clientBufferedWriter.close();
        scanner.close();
    }

    public static void main(String[] args) throws IOException {
        Client client1 = new Client();
//        client1.clientSendUDPThread.start();
//        client1.clientReceiveUDPThread.start();
        client1.clientSendTCPThread.start();
        client1.clientReceiveTCPThread.start();
    }
}
