package org.comroid.util;

import java.io.OutputStream;
import java.io.PrintStream;

public final class StreamUtil {
    private StreamUtil() {
        throw new UnsupportedOperationException();
    }

    public static OutputStream voidOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) {
            }
        };
    }

    public static PrintStream voidPrintStream() {
        return new PrintStream(voidOutputStream());
    }
}
