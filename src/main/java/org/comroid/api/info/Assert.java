package org.comroid.api.info;

import java.util.Objects;

public class Assert { // todo
    private Assert() {
        throw new UnsupportedOperationException();
    }

    public static void Equal(Object x, Object y) {
        if (!Objects.equals(x, y))
            throw exc("Internal error");
    }

    private static AssertionError exc(String msg) {
        return new AssertionError(msg);
    }
}
