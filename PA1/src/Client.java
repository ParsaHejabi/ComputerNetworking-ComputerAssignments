import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;

public class Client {
    private DatagramSocket socket;
    private InetAddress address;

    private byte[] buf;

    private void sendHttpGET_UDP(URL destinationUrl) throws IOException {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");

        String httpReq = "GET " + destinationUrl.getPath() + " HTTP/1.1\n";
        httpReq += "Host: " + destinationUrl.getHost() + "\n";
        httpReq += "Accept: text/html\n";
        httpReq += "Connection: close\n";
        //sending http request
        buf = httpReq.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        socket.send(packet);
        buf = new byte[4096];
        packet = new DatagramPacket(buf, buf.length);
        //receiving http response
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());

        System.out.println(received);

        //sending "end"
        packet = new DatagramPacket("end".getBytes(), "end".length(), address, 4445);
        socket.send(packet);
        socket.close();

        int htmlBegin = received.indexOf("Connection: close") + 17;

        BufferedWriter writer = new BufferedWriter(new FileWriter("index.html"));
        writer.write(received.substring(htmlBegin));
        writer.close();
    }

    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        URL destinationUrl = new URL("http://example.com/");
        Client c = new Client();
        c.sendHttpGET_UDP(destinationUrl);
    }
}
