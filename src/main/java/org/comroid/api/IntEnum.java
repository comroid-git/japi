package org.comroid.api;

public interface IntEnum {
    int getValue();

    default boolean equals(int value) {
        return getValue() == value;
    }
}
