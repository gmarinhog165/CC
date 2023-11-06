package cmd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Chunk {

    /**
     * array de bytes que vai conter os dados do file (max 1000 bytes -> MTU Ethernet)
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

    private int num;

    /**
     * Mensagem 2,3,4. -> 1490 bytes dedicados ao payload
     * @param data
     * @param len
     * @param offset
     * @param last
     * @param msg
     */
    public Chunk(byte[] data, int len, int offset, boolean last, byte msg, int num){
        this.data = Arrays.copyOf(data, len);
        this.length = len;
        this.offset = offset;
        this.last = last;
        this.msg = msg;
        this.num = num;
    }

    public Chunk(byte[] data, int len, int offset, boolean last, byte msg){
        this.data = Arrays.copyOf(data, len);
        this.length = len;
        this.offset = offset;
        this.last = last;
        this.msg = msg;
        this.num = 0;
    }

    /**
     * Mensagem 5 -> Desconexão de um Node ao Tracker
     * Inicializa todos os parâmetros nos default values para fazer deserialization
     * @param msg
     */
    public Chunk(byte msg) {
        this.msg = msg;
        this.data = new byte[0];
        this.length = 0;
        this.offset = 0;
        this.last = false;
        this.num = 0;
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

    public int getNum() {
        return num;
    }

    /**
     * Cria um byte[] a partir de um Chunk
     * @param chunk
     * @return
     */
    public static byte[] toByteArray(Chunk chunk) {
        int len = chunk.getLength();
        byte[] result = new byte[14 + len]; // 14 bytes for fields, plus payload length

        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Convert 'length' (4 bytes)
        buffer.putInt(len);

        int offset = chunk.getOffset();
        // Convert 'offset' (4 bytes)
        buffer.putInt(offset);

        boolean last = chunk.isLast();
        // Convert 'last' (1 byte)
        buffer.put((byte) (last ? 1 : 0));

        byte msg = chunk.getMsg();
        // Convert 'msg' (1 byte)
        buffer.put(msg);

        int num = chunk.getNum();
        // Convert 'num' (4 bytes)
        buffer.putInt(num);

        byte[] data = chunk.getData();
        // Copy 'data' into the result
        System.arraycopy(data, 0, result, 14, len);

        return result;
    }

    /**
     * Cria um Chunk manualmente a partir de um byte[]
     * @param bytes
     * @return
     */
    public static Chunk readByteArray(byte[] bytes) {
        if (bytes.length < 14) {
            throw new IllegalArgumentException("Input byte array is too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Extract 'length' (4 bytes)
        int length = buffer.getInt();

        // Validate the data size
        if (bytes.length < 14 + length) {
            throw new IllegalArgumentException("Incomplete data in the byte array");
        }

        // Extract 'offset' (4 bytes)
        int offset = buffer.getInt();

        // Extract 'last' (1 byte)
        boolean last = buffer.get() == 1;

        // Extract 'msg' (1 byte)
        byte msg = buffer.get();

        int num = buffer.getInt();

        // Extract 'data'
        byte[] data = Arrays.copyOfRange(bytes, 14, 14 + length);

        return new Chunk(data, length, offset, last, msg, num);
    }

    public static List<Chunk> fromByteArray(byte[] bytes, byte msg) {
        List<Chunk> chunks = new ArrayList<>();
        int offset = 0;
        int remainingLength = bytes.length;

        while (remainingLength > 0) {
            int chunkSize = Math.min(986, remainingLength);
            byte[] chunkData = Arrays.copyOfRange(bytes, offset, offset + chunkSize);
            boolean isLast = remainingLength <= 986;

            Chunk chunk = new Chunk(chunkData, chunkSize, offset, isLast, msg);
            chunks.add(chunk);

            offset += chunkSize;
            remainingLength -= chunkSize;
        }

        return chunks;
    }

    /**
     * Método que diz quantos chunks um file precisa.
     * @param length -> length de um byte[]
     * @return
     */
    public static int numChunks(int length){
        return (int) Math.ceil((double) length / 986);
    }

    /**
     * Método que devolve o offset dum bloco de dados a partir do seu index
     * @param index
     * @return
     */
    public static int findOffsetStartFromIndex(int index){
        return (int) index * 986;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Chunk chunk = (Chunk) o;

        if (getLength() != chunk.getLength()) {
            return false;
        }
        if (getOffset() != chunk.getOffset()) {
            return false;
        }
        if (isLast() != chunk.isLast()) {
            return false;
        }
        if (getMsg() != chunk.getMsg()) {
            return false;
        }
        if (getNum() != chunk.getNum()){
            return false;
        }
        return Arrays.equals(getData(), chunk.getData());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getLength(), getOffset(), isLast(), getMsg(), getNum());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }

    public static byte[] convertChunksToByteArray(List<Chunk> chunkList) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        for (Chunk chunk : chunkList) {
            byte[] chunkData = Chunk.toByteArray(chunk);
            try {
                byteArrayOutputStream.write(chunkData);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception as needed
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

}


