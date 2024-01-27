package org.comroid.api.net;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.comroid.api.attr.Named;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Range;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class EncryptionUtil {
    private final Map<UUID, Cipher> cipherCache = new ConcurrentHashMap<>();

    @lombok.Builder
    @SneakyThrows
    public Cipher prepareCipher(
            UUID id,
            Algorithm algorithm,
            Transformation transformation,
            @MagicConstant(valuesFromClass = Cipher.class) int mode,
            @Range(from = 16, to = 16) String key
    ) {
        return cipherCache.computeIfAbsent(id, $->$createCipher(algorithm, transformation, mode, key));
    }

    @SneakyThrows
    private Cipher $createCipher(
            Algorithm algorithm,
            Transformation transformation,
            @MagicConstant(valuesFromClass = Cipher.class) int mode,
            @Range(from = 16, to = 16) String key
    ) {
        var secret = new SecretKeySpec(key.getBytes(), algorithm.getName());
        var cipher = Cipher.getInstance(transformation.getName());
        cipher.init(mode, secret);
        return cipher;
    }

    public enum Algorithm implements Named {
        AES, DES, DESede, RSA, Blowfish, RC4, RC5, RC6, IDEA, Twofish;
    }

    public enum Transformation implements Named {
        AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding"),
        AES_ECB_PKCS5Padding("AES/ECB/PKCS5Padding"),
        AES_GCM_NoPadding("AES/GCM/NoPadding"),
        DES_CBC_PKCS5Padding("DES/CBC/PKCS5Padding"),
        DES_ECB_PKCS5Padding("DES/ECB/PKCS5Padding"),
        DESede_CBC_PKCS5Padding("DESede/CBC/PKCS5Padding"),
        DESede_ECB_PKCS5Padding("DESede/ECB/PKCS5Padding"),
        RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding"),
        RSA_ECB_OAEPWithSHA_1AndMGF1Padding("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"),
        RSA_ECB_OAEPWithSHA_256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"),
        Blowfish_CBC_PKCS5Padding("Blowfish/CBC/PKCS5Padding"),
        Blowfish_ECB_PKCS5Padding("Blowfish/ECB/PKCS5Padding"),
        RC4("RC4"),
        RC5("RC5"),
        RC6("RC6"),
        IDEA_CBC_PKCS5Padding("IDEA/CBC/PKCS5Padding"),
        IDEA_ECB_PKCS5Padding("IDEA/ECB/PKCS5Padding"),
        Twofish_CBC_PKCS5Padding("Twofish/CBC/PKCS5Padding"),
        Twofish_ECB_PKCS5Padding("Twofish/ECB/PKCS5Padding");

        @Getter
        private final String name;

        Transformation(String name) {
            this.name = name;
        }
    }
}
