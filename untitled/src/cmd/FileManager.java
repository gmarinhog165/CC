package cmd;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            file.read(data, 0, length);
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

    public static void createEmptyFile(String path) throws IOException {
        Files.createFile(Paths.get(path));
    }

    /**
     * método que remove o path e fica só o nome do file
     * @param path
     * @return
     */
    public static String getFileName(String path){
        Path path1 = Paths.get(path);
        return path1.getFileName().toString();
    }

    public static String extractFilePath(String input) {
        // Define a pattern to match "GET " followed by a file path
        Pattern pattern = Pattern.compile("^GET\\s+(.+)$");
        Matcher matcher = pattern.matcher(input);

        // Check if the pattern matches the input
        if (matcher.matches()) {
            // Group 1 contains the file path
            return matcher.group(1);
        }

        // Return null for invalid input format
        return null;
    }

    public static List<byte[]> intoListByteArray(byte[] input) {
        int maxSize = 986;
        if (input == null) {
            throw new IllegalArgumentException("Invalid input");
        }

        List<byte[]> result = new ArrayList<>();

        int offset = 0;
        int length = Math.min(maxSize, input.length);

        while (offset < input.length) {
            byte[] chunk = new byte[length];
            System.arraycopy(input, offset, chunk, 0, length);
            result.add(chunk);

            offset += length;
            length = Math.min(maxSize, input.length - offset);
        }

        return result;
    }

//    public static byte[] readAllFile(String filePath) throws IOException {
//        Path path = Paths.get(filePath);
//        return Files.readAllBytes(path);
//    }

    public static byte[] readAllFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             var inputStream = Files.newInputStream(path)) {

            byte[] buffer = new byte[4096]; // Adjust the buffer size as needed

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }

    public static List<byte[]> calculateSHA1Chunks(String filePath, int chunkSize) {
        List<byte[]> result = new ArrayList<>();

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(chunk);
                result.add(sha1);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }

        return result;
    }


}
