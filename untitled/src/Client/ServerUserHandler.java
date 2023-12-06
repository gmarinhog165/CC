package Client;

import cmd.Chunk;
import cmd.ConnectionTCP;
import cmd.FileManager;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ServerUserHandler implements Runnable {
    private Socket socket;
    private String file_path;
    private List<String> file_Wanted = new ArrayList<>();
    private ConnectionTCP contcp;
    private DNStable dns;
    private Map<String, Long> rtts = new HashMap<>();


    public ServerUserHandler(Socket socket, String path, DNStable dns) throws IOException {
        this.socket = socket;
        this.file_path = path;
        this.contcp = new ConnectionTCP(socket);
        this.dns = dns;
    }

    /**
     * Método responsável por interagir com o utilizador e responder consoante isso.
     */
    public void run() {
        try (Scanner scan = new Scanner(System.in)) {
            // enviar o nome do file e nr de chunks (MENSAGEM 1)
            byte[] path = file_path.getBytes(StandardCharsets.UTF_8);
            int numchunk = FileManager.howManyBytesFileHas(file_path);
            if (numchunk == 0){
                System.out.println("Conexão não autorizada! O ficheiro não existe!");
                return;
            }

            contcp.send(new Chunk(path, path.length, 0, true, (byte) 1, numchunk));


            // Preparação para mandar a lista com SHA-1 de cada chunk
            List<byte[]> shas = (FileManager.intoListByteArray(FileManager.readAllFile(file_path)));
            List<byte[]> sha1List = shas.stream().map(bytes -> {
                try { return MessageDigest.getInstance("SHA-1").digest(bytes); }
                catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
            }).toList();
            int len = sha1List.size();
            int m = 0;
            List<Chunk> chunkssha1 = new ArrayList<>();
            for(byte[] c : sha1List){
                Chunk d;
                if(m == len-1)
                    d= new Chunk(c, c.length,0,true,(byte) 2, m+1);
                else
                    d= new Chunk(c, c.length,0,false,(byte) 2, m+1);
                chunkssha1.add(d);
                m++;
            }
            contcp.send(new Chunk(path, path.length, chunkssha1.size(), false, (byte) 2));
            for(Chunk k : chunkssha1){
                contcp.send(k);
                contcp.receive();
            }


            contcp.send(new Chunk((byte) 11));
            int sizeList = contcp.receive().getOffset();
            List<String> cnks = new ArrayList<>();
            for(int i = 0; i < sizeList; i++){
                cnks.add(new String(contcp.receive().getData()));
                contcp.send(new Chunk((byte) 9));
            }

            Thread pp = new Thread(new RTTHandler(this.dns, this.rtts, cnks));
            pp.start();



            boolean loop = true;
            while (loop) {
                // loop para o utilizador colocar comandos corretos, quando coloca dá break
                while (true) {
                    String input = scan.nextLine();
                    Chunk message = inputMessageManager(input);
                    if (message == null) {
                        System.out.println("O comando introduzido é inválido!");
                    } else if (message.getMsg() == (byte) 8){
                        contcp.send(message);
                        //System.exit(0);
                    }
                    else {
                        contcp.send(message);
                        break;
                    }
                }

                List<Chunk> chunksDoMap = new ArrayList<>();
                while (true) {
                    Chunk data = contcp.receive();

                    if(data.getMsg() == (byte) 7){
                        System.out.println("O ficheiro que inseriu não está disponível!");
                        break;
                    }
                    else if(data.getMsg() == (byte) 8){
                        loop = false;
                        break;
                    }
                    else if(data.getMsg() == (byte) 3){
                        int i = data.getLength();
                        for(int t = 0; t < i; t++){
                            chunksDoMap.add(contcp.receive());
                            contcp.send(new Chunk((byte) 9));
                        }

                        List<Chunk> toDS = new ArrayList<>();
                        int len2 = contcp.receive().getLength();
                        for(int t = 0; t < len2; t++){
                            toDS.add(contcp.receive());
                            contcp.send(new Chunk((byte) 9));
                        }

                        String path1 = this.file_Wanted.get(0);
                        Thread toexec = new Thread(new TaskManager(this.contcp, chunksDoMap, path1, toDS, this.dns));
                        toexec.start();
                        this.file_Wanted.remove(0);
                        break;
                    }

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
    private Chunk inputMessageManager(String input){
        Chunk ret = null;
        if(input.contains("GET ")){
            String file = FileManager.extractFilePath(input);
            this.file_Wanted.add(file);
            byte[] data = file.getBytes(StandardCharsets.UTF_8);
            ret = new Chunk(data,data.length,0,true,(byte) 3);
        }
        else if(input.toLowerCase().contains("exit")){
            ret = new Chunk((byte) 8);
        }

        return ret;
    }
}
