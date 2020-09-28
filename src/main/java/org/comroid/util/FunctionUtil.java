package org.comroid.util;

import java.util.concurrent.Callable;

public final class FunctionUtil {
    public static <T> T executeRethrow(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
