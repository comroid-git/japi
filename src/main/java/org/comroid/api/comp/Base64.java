package org.comroid.api.comp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static java.util.Base64.*;

public final class Base64 {
    public static String encode(String string) {
        if (string == null)
            return null;
        return getEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String string) {
        if (string == null)
            return null;
        return new String(getDecoder().decode(string.getBytes(StandardCharsets.UTF_8)));
    }
}
