package org.comroid.util;

import lombok.experimental.UtilityClass;

import java.io.OutputStream;
import java.io.PrintStream;

@UtilityClass
public final class StreamUtil {
    public static PrintStream voidPrintStream() {
        return new PrintStream(voidOutputStream());
    }

    public static OutputStream voidOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) {
            }
        };
    }
}
