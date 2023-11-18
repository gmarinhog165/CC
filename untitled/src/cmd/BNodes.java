package cmd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class BNodes {
    private byte[] data;
    private int length;
    private int nchunk;
    private int readlen;
    private byte msg;

    public BNodes(byte[] data, int length, int nchunk, int readlen, byte msg) {
        this.data = data;
        this.length = length;
        this.msg = msg;
        this.readlen = readlen;
        this.nchunk = nchunk;
    }

    public byte[] getData() {
        return Arrays.copyOf(this.data, this.length);
    }

    public int getLength() {
        return length;
    }

    public byte getMsg() {
        return msg;
    }

    public int getNchunk() {
        return nchunk;
    }

    public int getReadlen() {
        return readlen;
    }

    /**
     * Método que faz serialize
     * @param bNodes
     * @return
     */
    public static byte[] toByteArray(BNodes bNodes) {
        int len = bNodes.getLength();
        byte[] result = new byte[13 + len]; // 6 bytes for fields, plus payload length

        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Convert 'length' (4 bytes)
        buffer.putInt(len);

        byte msg = bNodes.getMsg();
        // Convert 'msg' (1 byte)
        buffer.put(msg);

        int nchunk = bNodes.getNchunk();
        buffer.putInt(nchunk);

        int readlen = bNodes.getReadlen();
        buffer.putInt(readlen);

        byte[] data = bNodes.getData();
        // Copy 'data' into the result
        System.arraycopy(data, 0, result, 13, len);

        return result;
    }

    /**
     * Método que faz deserialize
     * @param bytes
     * @return
     */
    public static BNodes readByteArray(byte[] bytes) {
        if (bytes.length < 13) {
            throw new IllegalArgumentException("Input byte array is too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Extract 'length' (4 bytes)
        int length = buffer.getInt();

        // Validate the data size
        if (bytes.length < 13 + length) {
            throw new IllegalArgumentException("Incomplete data in the byte array");
        }

        // Extract 'msg' (1 byte)
        byte msg = buffer.get();
        int nchunk = buffer.getInt();
        int readlen = buffer.getInt();

        // Extract 'data'
        byte[] data = new byte[length];
        buffer.get(data);

        return new BNodes(data, length, nchunk, readlen, msg);
    }
}
