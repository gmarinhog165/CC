package Server;

public class Main {
    public static void main(String[] args){
        FS_Track_Protocol protocol = new FS_Track_Protocol();
        protocol.connection(args[0]);
    }
}
