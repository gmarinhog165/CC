package Client;

import java.net.UnknownHostException;

public class Main {
    // receber um path comum dum file /../home/core... e escrever na pasta de cada node n2.conf etc
    public static void main (String[] args) throws UnknownHostException {
        FS_Node node = new FS_Node(args[2]);
        node.connectionServerTCP(args[0], args[1]);
    }
}
