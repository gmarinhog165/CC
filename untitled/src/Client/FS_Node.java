package Client;

import cmd.Chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class FS_Node {
    private String file_path;

    public FS_Node(String file){
        this.file_path = file;
    }

    public void connectionServerTCP(String ip, String port) {
        int serverPort = Integer.parseInt(port);
        try {
            Socket socket = new Socket(ip, serverPort);
            System.out.println("Connected to the server at " + ip + ":" + serverPort);

            // send data

            Thread sendDataToTracker = new Thread(new ServerSend(socket, this.file_path));
            Thread receiveDataFromTracker = new Thread(new ServerReceive(socket));
            sendDataToTracker.start();
            receiveDataFromTracker.start();
            sendDataToTracker.join();

            socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
