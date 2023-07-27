package org.comroid.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.InputStream;
import java.security.MessageDigest;

@UtilityClass
public class MD5 {
    @SneakyThrows
    public String calculate(InputStream inputStream) {
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192]; // Buffer size can be adjusted as per your requirement
        int bytesRead;

        // Read data from the InputStream and update the MessageDigest
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md5Digest.update(buffer, 0, bytesRead);
        }

        // Calculate the MD5 hash
        byte[] md5Bytes = md5Digest.digest();

        // Convert the bytes to a hexadecimal representation
        StringBuilder md5Hex = new StringBuilder();
        for (byte b : md5Bytes) {
            String hexString = Integer.toHexString(0xFF & b);
            if (hexString.length() == 1) {
                md5Hex.append("0");
            }
            md5Hex.append(hexString);
        }

        return md5Hex.toString();
    }
}
