package Client;

public class Main {
    public static void main (String[] args){
        FS_Node node = new FS_Node();
        node.conexao(args[0], args[1]);
    }
}
