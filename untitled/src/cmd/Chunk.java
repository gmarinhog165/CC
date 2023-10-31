package cmd;

import java.io.*;
import java.util.Arrays;

public class Chunk {

    /**
     * array de bytes que vai conter os dados do file (max 1500 bytes -> MTU Ethernet)
     */
    private byte[] data; // dinâmico

    /**
     * Na mensagem 2 identifica o nº de chunks num file
     * Na mensagem 4 identifica o tamanho de byte[]
     */
    private int length; // 4bytes

    /**
     * Indica o offset da mensagem
     */
    private int offset; // 4bytes

    /**
     * Indica se é o ultimo chunk
     */
    private boolean last; // 1byte

    /**
     * Identifica o tipo de mensagem.
     */
    private byte msg; // 1 byte

    /**
     * Mensagem 2,3,4. -> 1490 bytes dedicados ao payload
     * @param data
     * @param len
     * @param offset
     * @param last
     * @param msg
     */
    public Chunk(byte[] data, int len, int offset, boolean last, byte msg){
        this.data = Arrays.copyOf(data, len);
        this.length = len;
        this.offset = offset;
        this.last = last;
        this.msg = msg;
    }

    /**
     * Mensagem 5 -> Desconexão de um Node ao Tracker
     * @param msg
     */
    public Chunk(byte msg){
        this.msg = msg;
    }

    public byte[] getData() {
        return Arrays.copyOf(this.data, this.length);
    }

    public int getLength() {
        return length;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isLast() {
        return last;
    }

    public short getMsg() {
        return msg;
    }

    public static Chunk deserializeObject(byte[] data) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object deserializedObject = objectInputStream.readObject();
            objectInputStream.close();
            return (Chunk) deserializedObject;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] serializeObject(Chunk chunk) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(chunk);
            objectOutputStream.flush();
            objectOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Error occurred
        }
    }

}


