package Client;

import java.io.IOException;
import java.net.Socket;

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

            Thread sendDataToTracker = new Thread(new ServerUserHandler(socket, this.file_path));
            // Thread de servidor UDP
            sendDataToTracker.start();
            sendDataToTracker.join();

            socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
