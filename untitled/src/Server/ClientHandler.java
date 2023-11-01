package Server;

import cmd.Chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;

public class ClientHandler implements Runnable{
    private Socket clientSocket;
    private FS_Tracker server;

    public ClientHandler(Socket clientSocket, FS_Tracker server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            // forma de ler dados do cliente, está correta?
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                // dar deserialize para chunk
                Chunk data = Chunk.deserializeObject(Arrays.copyOf(buffer, bytesRead));
                System.out.println("Received chunk with message " + new String(data.getData()) + " from IP: " + clientSocket.getInetAddress());

                // para responder
                // messageManager()
                byte[] response = "obrigado".getBytes();
                out.write(response);
                out.flush();
            }

            // fechar o socket qd acabar
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
