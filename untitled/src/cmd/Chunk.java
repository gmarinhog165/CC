package cmd;

import java.io.*;
import java.util.Arrays;

public class Chunk implements Serializable {

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
     * Inicializa todos os parâmetros nos default values para fazer deserialization
     * @param msg
     */
    public Chunk(byte msg) {
        this.msg = msg;
        this.data = new byte[0]; // Initialize 'data' to an empty byte array
        this.length = 0; // Initialize 'length' to 0
        this.offset = 0; // Initialize 'offset' to 0
        this.last = false; // Initialize 'last' to false
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

    public byte getMsg() {
        return msg;
    }

//    public static Chunk deserializeObject(byte[] data) {
//        try {
//            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
//            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
//            Object deserializedObject = objectInputStream.readObject();
//            objectInputStream.close();
//            return (Chunk) deserializedObject;
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    public static byte[] serializeObject(Chunk chunk) {
//        try {
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
//            objectOutputStream.writeObject(chunk);
//            objectOutputStream.flush();
//            objectOutputStream.close();
//            return byteArrayOutputStream.toByteArray();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null; // Error occurred
//        }
//    }

    // Manually convert the Chunk object to a byte array
    public static byte[] toByteArray(Chunk chunk) {
        int len = chunk.getLength();
        byte[] result = new byte[12 + len]; // 12 bytes for fields, plus payload length

        // Convert 'length' (4 bytes)
        result[0] = (byte) (len >> 24);
        result[1] = (byte) (len >> 16);
        result[2] = (byte) (len >> 8);
        result[3] = (byte) len;

        int offset = chunk.getOffset();
        // Convert 'offset' (4 bytes)
        result[4] = (byte) (offset >> 24);
        result[5] = (byte) (offset >> 16);
        result[6] = (byte) (offset >> 8);
        result[7] = (byte) offset;

        boolean last = chunk.isLast();
        // Convert 'last' (1 byte)
        result[8] = (byte) (last ? 1 : 0);

        byte msg = chunk.getMsg();
        // Convert 'msg' (1 byte)
        result[9] = msg;

        byte[] data = chunk.getData();
        // Copy 'data' into the result
        System.arraycopy(data, 0, result, 10, len);

        return result;
    }

    // Manually create a Chunk object from a byte array
    public static Chunk fromByteArray(byte[] bytes) {
        if (bytes.length < 12) {
            throw new IllegalArgumentException("Input byte array is too short");
        }

        // Extract 'length' (4 bytes)
        int length = (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];

        // Extract 'offset' (4 bytes)
        int offset = (bytes[4] << 24) | (bytes[5] << 16) | (bytes[6] << 8) | bytes[7];

        // Extract 'last' (1 byte)
        boolean last = bytes[8] == 1;

        // Extract 'msg' (1 byte)
        byte msg = bytes[9];

        // Extract 'data'
        byte[] data = Arrays.copyOfRange(bytes, 10, bytes.length);

        return new Chunk(data, length, offset, last, msg);
    }

}


