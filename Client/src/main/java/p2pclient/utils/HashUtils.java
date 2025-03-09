package p2pclient.utils;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    /**
     * Computes SHA-256 hash for either a full file or a given byte array (chunk).
     *
     * @param input Either a `File` object (full file) or `byte[]` (chunk).
     * @return The SHA-256 hash as a hexadecimal string.
     * @throws IOException If file reading fails.
     * @throws NoSuchAlgorithmException If SHA-256 algorithm is not available.
     */
    public static String computeHash(Object input) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        if (input instanceof File file) {
            try (InputStream fis = new FileInputStream(file);
                 DigestInputStream dis = new DigestInputStream(fis, digest)) {
                while (dis.read() != -1) {} // Read entire file
            }
        } else if (input instanceof byte[] chunkData) {
            // per byte
            digest.update(chunkData);
        } else {
            throw new IllegalArgumentException("Invalid input type. Must be File or byte[].");
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}