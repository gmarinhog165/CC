import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class FS_Tracker {
    Map<String, Map<Integer, String>> catalogo_chunks;

    public FS_Tracker(){
        this.catalogo_chunks = new HashMap<>();
    }

    public void Conexão(String port) {
        Chunk chunk = null;
        try{
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(port));
            System.out.println("Abri um socket e estou à escuta na porta: " + port);
            byte[] buffer = new byte[2048];

            while(true){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress remetente = packet.getAddress();
                Chunk data = Chunk.deserializeObject(buffer);
                System.out.println("Recebi o chunk cujo offset é " + data.getOffset() + " do IP: " + remetente);
            }
        } catch (SocketException e){

        } catch (IOException d){

        }

    }

    private void messageManager(byte msg){
        //TO DO
    }
}
