import java.io.*;
import java.net.Socket;
import java.net.URL;

public class Proxy {

    private static final int PORT_NUMBER = 80;

    public static void main(String[] args) throws IOException {
        URL destinationUrl = new URL("http://example.com/");

        Socket socket = new Socket(destinationUrl.getHost(), PORT_NUMBER);

        OutputStream outputStream = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("GET " + destinationUrl.getPath() + " HTTP/1.1");
        writer.println("Host: " + destinationUrl.getHost());
        writer.println("Accept: text/html");
        writer.println("Connection: close");
        writer.println();

        writer.flush();

        InputStream inputStream = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;

        while ((line = reader.readLine()) != null){
            System.out.println(line);
        }
        socket.close();
    }
}
