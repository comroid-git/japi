package org.comroid.api.attr;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

public interface Owned {
    static Object wrap(Object it) {
        return it instanceof Owned ? ((Owned) it).getOwner() : it;
    }

    Object getOwner();

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Base<T> implements Owned {
        T owner;
    }
}
