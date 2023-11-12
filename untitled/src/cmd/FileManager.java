package cmd;


import java.io.*;

public class FileManager {

    /**
     * Método que converte qualquer ficheiro num byte[]. (Para a fase inicial quando um Node se conecta)
     * @param filePath
     * @return
     * @throws IOException
     */
    public static byte[] fileToByteArray(String filePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            return byteArrayOutputStream.toByteArray();
        }
    }

    /**
     * Método para ler apenas um chunk de um ficheiro escrito, desde o offset com a length pretendida
     * @param filePath
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static byte[] readBytesFromFile(String filePath, int offset, int length) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            file.seek(offset);
            byte[] data = new byte[length];
            file.readFully(data);
            return data;
        }
    }


    /**
     * Método que vai escrever um segmento de byte[] no offset dado de forma a não ter de guardar
     * sempre em memória até ter todos os chunks
     * @param data
     * @param filePath
     * @param offset
     * @throws IOException
     */
    public static void writeBytesToFile(byte[] data, String filePath, int offset) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {
            file.seek(offset);
            file.write(data);
        }
    }

    /**
     * Método que a partir duma string com o nome do file guarda a extension do file
     * @param filename
     * @return
     */
    public static String getExtension(String filename){
        int lastDotIndex = filename.lastIndexOf(".");
        return (filename.substring(lastDotIndex));
    }

    /**
     * Método que retorna o nº de bytes dum file (Ceil@2gb) -> mudar para long se usarmos files maiores
     * @param filePath
     * @return
     */
    public static int howManyBytesFileHas(String filePath){
        File file = new File(filePath);

        if (file.exists()) {
            return (int) file.length();
        } else {
            return 0;
        }
    }


}
