package org.comroid.util;

import org.comroid.api.DelegateStream;

import java.util.List;
import java.util.Optional;

public final class StackTraceUtils {
    public static String callerString(StackTraceElement[] trace, int index) {
        var me = trace[index];
        var prev = trace[index - 1];
        var call = prev.getClassName();
        if (call.contains("."))
            call = call.substring(call.lastIndexOf('.') + 1);
        return "call to " + call + '.' + prev.getMethodName() + "() in " + me;
    }

    public static String caller(int skip) {
        try {
            var trace = new Throwable().getStackTrace();
            var basis = trace[skip];
            var filter = basis.getClassName();
            filter = filter.substring(filter.lastIndexOf('.') + 1);// + '.' + basis.getMethodName();
            if (filter.contains("$")) filter = filter.substring(filter.lastIndexOf('$'));
            for (int i = skip; i < trace.length; i++) {
                if (!trace[i].toString().contains(filter))
                    return callerString(trace,i);
            }
            return callerString(trace,1);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(String.format("Cannot skip %d classes", skip), e);
        }
    }

    public static Class<?> callerClass(int skip) {
        var className = StackTraceUtils.class.getCanonicalName();
        try {
            className = new Throwable().getStackTrace()[1 + skip].getClassName();
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

    public static String lessSimpleName(Class<?> type) {
        return Optional.ofNullable(type.getCanonicalName())
                .map(name->name.substring(type.getPackageName().length() + 1))
                .orElseGet(type::getSimpleName);
    }
}
