package org.comroid.api.func.util;

import lombok.experimental.UtilityClass;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.info.Log;
import org.comroid.util.BigotryFilter;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.comroid.api.Polyfill.exceptionLogger;

@UtilityClass
@lombok.extern.java.Log
public final class Debug {
    public static String[] DEBUG_ENV_KEYS = new String[]{"DEBUG", "DEBUG_ENV", "IS_DEBUG", "TRACE", "TRACE_ENV", "IS_TRACE"};
    public static BooleanSupplier[] IS_DEBUG_CHECKS = new BooleanSupplier[]{Debug::isDebugEnv};
    public static Logger logger = Log.get(Debug.class);

    static {
        BigotryFilter.init();
    }

    public static boolean isDebug() {
        return Arrays.stream(IS_DEBUG_CHECKS).allMatch(BooleanSupplier::getAsBoolean);
    }

    public static boolean isDebugEnv() {
        Map<String, String> env = System.getenv();
        return Arrays.stream(DEBUG_ENV_KEYS).anyMatch(env::containsKey);
    }

    public static void printIntegerBytes(Logger logger, @Nullable String title, int value) {
        byte[] bytes = ByteBuffer.allocate(4)
                .putInt(value)
                .array();
        printByteArrayDump(logger, String.format("Integer Dump of %s [%d]", title, value), bytes);
    }

    public static void printByteArrayDump(Logger logger, @Nullable String title, byte[] bytes) {
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < bytes.length; i++) {
            byte each = bytes[i];
            sb.append(createByteDump(null, each))
                    .append('\n');
        }
        logger.log(Level.FINER, (title == null ? "" : "Printing byte array dump of " + title) + sb.substring(0, sb.length() - 1));
    }

    public static String createByteDump(@Nullable String title, byte each) {
        StringBuilder binaryString = new StringBuilder(Integer.toUnsignedString(each, 2));
        while (binaryString.length() < 8)
            binaryString.insert(0, '0');
        return String.format("%s0x%2x [0b%s]\t", (title == null ? "" : "Creating byte dump of " + title + ':' + each + '\n'), each, binaryString);
    }

    public static String createObjectDump(Object it) {return createObjectDump(it, DataNode.of(it),0);}
    private static String createObjectDump(final Object target, final int rec) {
        final var sb = new StringBuilder();
    }
}
