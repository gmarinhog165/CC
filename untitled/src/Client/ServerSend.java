package Client;

import cmd.Chunk;
import cmd.FileManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerSend implements Runnable{
    private Socket socket;
    private String file_path;

    public ServerSend(Socket socket, String path) {
        this.socket = socket;
        this.file_path = path;
    }

    /**
     * Método responsável por interagir com o utilizador e responder consoante isso.
     */
    public void run() {
        try (Scanner scan = new Scanner(System.in)) {
            // enviar o nome do file e nr de chunks (MENSAGEM 1)
            byte[] path = file_path.getBytes();
            int numchunk = FileManager.howManyChunksFileHas(file_path);
            if (numchunk == 0)
                return;
            Chunk haveFile = new Chunk(path, path.length, 0, true, (byte) 1, numchunk);
            OutputStream out = socket.getOutputStream();
            out.write(Chunk.toByteArray(haveFile));
            out.flush();

            while (true) {
                String input = scan.nextLine();
                List<Chunk> message = inputMessageManager(input);
                if (message == null) {
                    System.out.println("O comando introduzido é inválido!");
                } else {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    for (Chunk c : message) {
                        byte[] data = Chunk.toByteArray(c);
                        byteArrayOutputStream.write(data, 0, data.length);
                    }

                    // envia os bytes todos, depois o tcp parte
                    byte[] serializedData = byteArrayOutputStream.toByteArray();
                    out.write(serializedData);
                    out.flush();
                    if (message.size() == 1 && message.get(0).getMsg() == (byte) 8)
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Método que devolve uma lista de Chunk consoante a mensagem introduzida pelo utilizador
     * @param input
     * @return
     */
    private List<Chunk> inputMessageManager(String input){
        List<Chunk> ret = null;
        if(input.contains("GET ")){
            String file = input.replace("GET ", "");
            byte[] data = file.getBytes();
            ret = Chunk.fromByteArray(data, (byte) 3);
        }
        else if(input.toLowerCase().contains("exit")){
            Chunk tmp = new Chunk((byte) 8);
            ret = new ArrayList<>();
            ret.add(tmp);
        }

        return ret;
    }
}
