package org.comroid.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class Debug {
    public static final Logger logger = LogManager.getLogger("Debug Tools");
    public static String[] DEBUG_ENV_KEYS = new String[]{"DEBUG", "DEBUG_ENV", "IS_DEBUG"};
    public static BooleanSupplier[] IS_DEBUG_CHECKS = new BooleanSupplier[]{Debug::isDebugEnv};

    public static boolean isDebug() {
        return Arrays.stream(IS_DEBUG_CHECKS).allMatch(BooleanSupplier::getAsBoolean);
    }

    public static boolean isDebugEnv() {
        Map<String, String> env = System.getenv();
        return Arrays.stream(DEBUG_ENV_KEYS).anyMatch(env::containsKey);
    }

    private Debug() {
        throw new UnsupportedOperationException("Debug is a Utility Class");
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
        logger.log(Level.ALL, (title == null ? "" : "Printing byte array dump of " + title) + sb.substring(0, sb.length() - 1));
    }

    public static String createByteDump(@Nullable String title, byte each) {
        StringBuilder binaryString = new StringBuilder(Integer.toUnsignedString(each, 2));
        while (binaryString.length() < 8)
            binaryString.insert(0, '0');
        return String.format("%s0x%2x [0b%s]\t", (title == null ? "" : "Creating byte dump of " + title + ':' + each + '\n'), each, binaryString);
    }
}
