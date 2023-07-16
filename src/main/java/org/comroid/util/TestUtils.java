package org.comroid.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.*;
import java.util.Random;

@UtilityClass
public class TestUtils {
    public static final Random rng = new Random();

    @SneakyThrows
    public String fillWithText(File file, int length) {
        var buf = new StringWriter();
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            while (length-- > 0) {
                int c = rng.nextInt('a', 'Z');
                buf.append((char) c);
                fos.write(c);
            }

            fos.flush();
            return buf.toString();
        }
    }
}
