import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Proxy extends Thread {

    private static final int PORT_NUMBER = 80;

    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[4096];
    private String end = "end";

    public void run() {
        try {
            socket = new DatagramSocket(4445);
            running = true;

            while (running) {
                buf = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                //Receiving packet from client (UDP)
                socket.receive(packet);
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                String received = new String(packet.getData(), 0, packet.getLength());
                //System.out.println("Proxy received: " + received);

                if (received.substring(0, 3).equals(end)) {
                    running = false;
                    continue;
                }

                String urlHost;
                int hostBegin = received.indexOf("Host: ") + 6;
                int hostEnd = received.indexOf("Accept: text/html") - 1;
                urlHost = received.substring(hostBegin, hostEnd);

                String httpRes = getHtml_TCP(urlHost, received);
                packet = new DatagramPacket(httpRes.getBytes(), httpRes.length(), address, port);
                socket.send(packet);

            }
            socket.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }

    }

    private static String getHtml_TCP(String urlHost, String httpReq) throws IOException {
        Socket socket = new Socket(urlHost, PORT_NUMBER);

        OutputStream outputStream = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println(httpReq);
        writer.flush();

        /*writer.println("GET " + destinationUrl.getPath() + " HTTP/1.1");
        writer.println("Host: " + destinationUrl.getHost());
        writer.println("Accept: text/html");
        writer.println("Connection: close");
        writer.println();

        writer.flush();*/

        InputStream inputStream = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line1, line2 = null;

        while ((line1 = reader.readLine()) != null) {
            line2 += line1 + "\n";
        }
        socket.close();
        return line2;
    }

    public static void main(String[] args){
        new Proxy().start();
    }
}
