package org.comroid.commands.autofill.impl;

public interface NamedAutoFillAdapter<T extends org.comroid.api.attr.Named> extends ObjectAutoFillAdapter<T> {
    @Override
    default String toString(T object) {
        return object.getPrimaryName();
    }
}
