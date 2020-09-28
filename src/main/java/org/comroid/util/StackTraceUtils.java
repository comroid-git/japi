package org.comroid.util;

import java.util.List;

public final class StackTraceUtils {
    public static Class<?> callerClass(int skip) {
        final String className = new Throwable().getStackTrace()[1 + skip].getClassName();

        try {
            return Class.forName(className);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(String.format("Cannot skip %d classes", skip), e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(String.format("Class not found: %s", className), e);
        }
    }

    public static void putStackTrace(
            final List<String> lines, final Throwable throwable, final int omitAt, final boolean recursive
    ) {
        if (throwable == null) {
            return;
        }

        lines.add(String.format(
                "%s: %s",
                throwable.getClass()
                        .getName(),
                throwable.getMessage()
        ));

        final StackTraceElement[] stackTrace = throwable.getStackTrace();

        for (int c = 0; c < stackTrace.length && (omitAt == -1 || c < omitAt); c++) {
            lines.add(stackTrace[c].toString());
        }

        if (recursive) {
            Throwable cause = throwable.getCause();
            if (cause != null) {
                putStackTrace(lines, cause, omitAt, recursive);
            }
        }
    }
}
