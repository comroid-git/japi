package org.comroid.util;

import java.util.Random;

public class Token {
    public static Random rng = new Random();

    public static String random(int length, boolean base64) {
        var str = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = 0;
            byte[] b = new byte[1];
            while (!Character.isDigit(c) && !Character.isLetter(c)){
                c=(char)(rng.nextInt(26)+'A');
            }
            str.append(c);
        }
        //if (base64) str = new StringBuilder(Base64.encode(str.toString()));
        return str.toString();
    }
}
