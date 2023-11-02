package Client;

import cmd.Chunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerSend implements Runnable{
    private Socket socket;

    public ServerSend(Socket socket) {
        this.socket = socket;
    }

    /**
     * Método responsável por interagir com o utilizador e responder consoante isso.
     */
    public void run() {
        try {
            Scanner scan = new Scanner(System.in);
            while(true){
                String input = scan.nextLine();
                List<Chunk> message = inputMessageManager(input);

                if(message.size() == 1 && message.get(0).getMsg() == 4)
                    break;

                if(message == null){
                    System.out.println("O comando introduzido é inválido!");
                }
                else{
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    for(Chunk c : message){
                        byte[] data = Chunk.toByteArray(c);
                        byteArrayOutputStream.write(data, 0, data.length);
                    }

                    // envia os bytes todos, está correto ou devia enviar chunk a chunk?
                    byte[] serializedData = byteArrayOutputStream.toByteArray();
                    OutputStream out = socket.getOutputStream();
                    out.write(serializedData);
                    out.flush();
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
            ret = Chunk.fromPath(data);
        }
        else if(input.toLowerCase().contains("exit")){
            Chunk tmp = new Chunk((byte) 4);
            ret = new ArrayList<>();
            ret.add(tmp);
        }

        return ret;
    }
}
