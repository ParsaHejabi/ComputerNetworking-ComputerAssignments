import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String projectPath = new File("").getAbsolutePath();
    private static final String HTMLFilesFolder = projectPath + "/HTMLFiles/";
    private static final String HTMLExtension = ".html";

    private DatagramSocket clientUDPSocket;
    private InetAddress proxyAddress;

    private URL destinationURL;

    private byte[] clientUDPBuffer;

    private Thread clientSendUDPThread;
    private Thread clientReceiveUDPThread;
    private Thread clientSendTCPThread;
    private Thread clientReceiveTCPThread;

    private BufferedWriter clientBufferedWriter;
    private Scanner scanner;

    Client() throws IOException {
        clientUDPSocket = new DatagramSocket();
        proxyAddress = InetAddress.getByName(Proxy.PROXY_ADDRESS);

        clientUDPBuffer = new byte[4096];

        scanner = new Scanner(System.in);

        clientSendUDPThread = new Thread(() -> {
            try {
                this.sendHttpGET_UDP();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        clientReceiveUDPThread = new Thread(() -> {
            try {
                receiveHttpGET_UDP();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendHttpGET_UDP() throws IOException {
        //TODO Change this condition!
        while (true) {
            System.out.println("Enter host name to send HTTP UDP packet to proxy: ");
            String s = scanner.next();
            if (s.equals("END")) {
                close();
                break;
            }
            this.destinationURL = new URL(s);

            String httpReq = "GET " + destinationURL.getPath() + " HTTP/1.1\n";
            httpReq += "Host: " + destinationURL.getHost() + "\n";
            httpReq += "Accept: text/html\n";
            httpReq += "Connection: close\n";

            //sending http udp request
            clientUDPBuffer = httpReq.getBytes();
            DatagramPacket clientUDPPacket = new DatagramPacket(clientUDPBuffer, clientUDPBuffer.length, proxyAddress, Proxy.UDP_PORT_NUMBER);
            clientUDPSocket.send(clientUDPPacket);
        }
    }

    private void receiveHttpGET_UDP() throws IOException {
        //TODO Change this condition!
        while (true) {
            //receiving http udp response
            DatagramPacket clientUDPPacket = new DatagramPacket(clientUDPBuffer, clientUDPBuffer.length);
            clientUDPSocket.receive(clientUDPPacket);
            String received = new String(clientUDPPacket.getData(), 0, clientUDPPacket.getLength());

            //DEBUGGING PURPOSE
            System.out.println(received);

            int htmlBegin = received.indexOf("\n\n") + 2;

            clientBufferedWriter = new BufferedWriter(new FileWriter(HTMLFilesFolder + this.destinationURL.getHost() + HTMLExtension));
            clientBufferedWriter.write(received.substring(htmlBegin));
            clientBufferedWriter.flush();
        }
    }

    private void close() throws IOException {
        clientUDPSocket.close();
        clientBufferedWriter.close();
        scanner.close();
    }

    public static void main(String[] args) throws IOException{
        Client client1 = new Client();
        client1.clientSendUDPThread.start();
        client1.clientReceiveUDPThread.start();
    }
}
