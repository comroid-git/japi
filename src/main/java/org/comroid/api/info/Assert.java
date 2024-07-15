package org.comroid.api.info;

import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class Assert { // todo
    public static void Equal(Object x, Object y) {
        if (!Objects.equals(x, y))
            throw exc("Internal error");
    }

    private AssertionError exc(String msg) {
        return new AssertionError(msg);
    }
}
