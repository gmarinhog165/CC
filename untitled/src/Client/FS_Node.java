package Client;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;

import cmd.Chunk;

public class FS_Node {

    public void connectionServerTCP(String ip, String port) {
        int serverPort = Integer.parseInt(port);
        try {
            Socket socket = new Socket(ip, serverPort);
            System.out.println("Connected to the server at " + ip + ":" + serverPort);

            // Create and send data as needed
            byte[] dados = "notas.md".getBytes();
            Chunk data = new Chunk(dados, dados.length, 0, true, (byte) 2);
            byte[] serializedData = Chunk.serializeObject(data);

            OutputStream out = socket.getOutputStream();
            out.write(serializedData);
            out.flush();

            // Receive data from the server
            InputStream in = socket.getInputStream();
            byte[] receiveBuffer = new byte[2048]; // Adjust the buffer size as needed
            int bytesRead;

            while ((bytesRead = in.read(receiveBuffer)) != -1) {
                // Process the received data from the server
                byte[] receivedData = Arrays.copyOf(receiveBuffer, bytesRead);
                //Chunk receivedData = Chunk.deserializeObject(Arrays.copyOf(receiveBuffer, bytesRead));
                System.out.println("Received chunk with message " + new String(receivedData));
            }

            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
