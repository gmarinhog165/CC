package Server;

import cmd.Chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

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
            byte[] buffer = new byte[1000]; // limitamos em 1000 para o chunk nao exceder 1000 bytes no deserialize
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {

                // dar deserialize para chunk
                Chunk data = Chunk.fromByteArray(Arrays.copyOf(buffer, bytesRead));
                System.out.println("Received chunk with message " + new String(data.getData()) + " from IP: " + clientSocket.getInetAddress());

                // exit message
                if(data.getMsg() == (byte) 4)
                    break;

                // para responder
                // messageManager()
                byte[] response = "obrigado".getBytes();
                out.write(response);
                out.flush();
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
