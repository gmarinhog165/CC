package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FS_Track_Protocol {
    private FS_Tracker server;

    public FS_Track_Protocol(){
        this.server = new FS_Tracker();
    }


    public void connection (String port) {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
            System.out.println("Server is listening on port " + port);

            while (true) {
                // aceita a conexão do cliente
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Cria um thread para cada cliente
                Thread clientThread = new Thread(new ClientHandler(clientSocket, server));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void messageManager(byte msg){
        //TO DO
    }
}
