package Client;
import java.net.*;
import cmd.Chunk;
public class FS_Node {

    public void conexao(String ip, String port){
        try{
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(port));
            System.out.println("Abri um socket e estou à escuta na porta: " + port);
            byte[] buffer = new byte[2048];
            byte[] dados = "notas.md".getBytes();

            InetAddress serverAddress = InetAddress.getByName(ip);
            Chunk data = new Chunk(dados, dados.length, 0, true, (byte) 2);
            byte[] dados2 = Chunk.serializeObject(data);
            DatagramPacket sendPacket = new DatagramPacket(dados2, dados2.length, serverAddress, Integer.parseInt(port));
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
