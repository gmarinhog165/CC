package Client;

import java.net.UnknownHostException;

public class Main {
    public static void main (String[] args) throws UnknownHostException {
        FS_Node node = new FS_Node();
        node.connectionServerTCP(args[0], args[1]);
    }
}
