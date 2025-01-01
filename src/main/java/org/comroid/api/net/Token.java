package org.comroid.api.net;

import org.comroid.api.comp.Base64;

import java.util.Random;
import java.util.function.Predicate;

public class Token {
    public static Random rng = new Random();

    public static String random(int length, boolean base64) {
        var str = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = 0;
            while (!Character.isDigit(c) && !Character.isLetter(c)) {
                c = (char) (rng.nextInt(26) + 'A');
            }
            str.append(c);
        }
        return base64 ? Base64.encode(str.toString()) : str.toString();
    }

    public static String generate(int length, boolean base64, Predicate<String> criteria) {
        String str;
        do {
            str = random(length, base64);
        } while (!criteria.test(str));
        return str;
    }
}
