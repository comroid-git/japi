package org.comroid.api.attr;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public interface StringAttribute {
    static <T extends Enum<?> & StringAttribute> Optional<T> valueOf(String value, Class<T> type) {
        return Arrays.stream(type.getEnumConstants())
                .filter(x -> Objects.equals(value, x.getString()))
                .findAny();
    }

    String getString();
}
