package org.comroid.api.attr;

public interface Index {
    default int index() {
        if (this instanceof Enum<?> e)
            return e.ordinal();
        throw new AbstractMethodError();
    }
}
