package org.comroid.api.func.util;

import lombok.experimental.UtilityClass;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.comp.StringBasedComparator;
import org.comroid.api.info.Log;
import org.comroid.util.BigotryFilter;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;
import static org.comroid.api.java.StackTraceUtils.lessSimpleName;

@UtilityClass
@lombok.extern.java.Log
public final class Debug {
    public static String[]          DEBUG_ENV_KEYS  = new String[]{ "DEBUG", "DEBUG_ENV", "IS_DEBUG", "TRACE", "TRACE_ENV", "IS_TRACE" };
    public static BooleanSupplier[] IS_DEBUG_CHECKS = new BooleanSupplier[]{ Debug::isDebugEnv };
    public static Logger            logger          = Log.get(Debug.class);

    static {
        try {
            BigotryFilter.init();
        } catch (Throwable ignored) {
        }
    }

    public static boolean isDebug() {
        return Cache.get("Debug.isDebug()",
                         () -> Arrays.stream(IS_DEBUG_CHECKS).allMatch(BooleanSupplier::getAsBoolean));
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

    public static String createObjectDump(Object it) {
        return "Dump: " + it + "\n" + createObjectDump(DataNode.of(it), 0);
    }

    public static void log(Logger log, String message) {log(log, message, null);}

    public static void log(Logger log, String message, @Nullable Throwable t) {log(log, message, Level.FINE, Level.WARNING, t);}

    public static void log(Logger log, String message, Level normalLevel, Level debugLevel) {log(log, message, normalLevel, debugLevel, null);}

    public static void log(Logger log, String message, Level normalLevel, Level debugLevel, @Nullable Throwable t) {
        log.log(isDebug() ? debugLevel : normalLevel, message, t);
    }

    private static String pad(int r, int c) {
        return range(0, r).mapToObj($ -> "|\t")
                .collect(Collectors.joining()) + "|-> ";
    }

    private static String createObjectDump(DataNode data, int rec) {
        var c = 0;
        final var result = new StringBuilder();

        if (data instanceof DataNode.Value<?> val)
            result.append("(").append(lessSimpleName(val.getHeldType().getTargetClass())).append(") ")
                    .append(val).append('\n');
        else {
            result.append(lessSimpleName(data.getClass()))
                    .append("[").append(data.size()).append("]")
                    .append("#").append(Integer.toHexString(data.hashCode()))
                    .append("\n");
            if (data instanceof DataNode.Object obj) {
                // append name, then values
                for (var entry : obj.entrySet().stream()
                        .sorted(Comparator.<Map.Entry<String, DataNode>>comparingInt(x -> x.getValue().size())
                                        .thenComparing(new StringBasedComparator<>(Map.Entry::getKey))).toList())
                    result.append(pad(rec, c++)).append(entry.getKey()).append(": ")
                            .append(createObjectDump(entry.getValue(), rec + 1)).append('\n');
            } else if (data instanceof DataNode.Array arr) {
                // append type, then values
                for (int i = 0; i < arr.size(); i++)
                    result.append(pad(rec, c++)).append("[").append(i).append("]: ")
                            .append(createObjectDump(arr.get(i), rec + 1)).append('\n');
            }
        }
        return result.toString().replaceAll("\n+", "\n");
    }
}
