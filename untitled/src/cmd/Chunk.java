package cmd;

import java.io.ByteArrayOutputStream;
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
        this.data = new byte[0];
        this.length = 0;
        this.offset = 0;
        this.last = false;
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


    /**
     * Cria um byte[] a partir de um Chunk
     * @param chunk
     * @return
     */
    public static byte[] toByteArray(Chunk chunk) {
        int len = chunk.getLength();
        byte[] result = new byte[10 + len]; // 12 bytes for fields, plus payload length

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

        byte[] data = chunk.getData();
        // Copy 'data' into the result
        System.arraycopy(data, 0, result, 10, len);

        return result;
    }

    /**
     * Cria um Chunk manualmente a partir de um byte[]
     * @param bytes
     * @return
     */
    public static Chunk readByteArray(byte[] bytes) {
        if (bytes.length < 10) {
            throw new IllegalArgumentException("Input byte array is too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Extract 'length' (4 bytes)
        int length = buffer.getInt();

        // Validate the data size
        if (bytes.length < 10 + length) {
            throw new IllegalArgumentException("Incomplete data in the byte array");
        }

        // Extract 'offset' (4 bytes)
        int offset = buffer.getInt();

        // Extract 'last' (1 byte)
        boolean last = buffer.get() == 1;

        // Extract 'msg' (1 byte)
        byte msg = buffer.get();

        // Extract 'data'
        byte[] data = Arrays.copyOfRange(bytes, 10, 10 + length);

        return new Chunk(data, length, offset, last, msg);
    }

    public static List<Chunk> fromByteArray(byte[] bytes, byte msg) {
        List<Chunk> chunks = new ArrayList<>();
        int offset = 0;
        int remainingLength = bytes.length;

        while (remainingLength > 0) {
            int chunkSize = Math.min(990, remainingLength);
            byte[] chunkData = Arrays.copyOfRange(bytes, offset, offset + chunkSize);
            boolean isLast = remainingLength <= 990;

            Chunk chunk = new Chunk(chunkData, chunkSize, offset, isLast, msg);
            chunks.add(chunk);

            offset += chunkSize;
            remainingLength -= chunkSize;
        }

        return chunks;
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
        return Arrays.equals(getData(), chunk.getData());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getLength(), getOffset(), isLast(), getMsg());
        result = 31 * result + Arrays.hashCode(getData());
        return result;
    }

}


