package org.comroid.api;

import org.comroid.util.StandardValueType;

import java.io.Reader;
import java.io.StringReader;

public interface StringSerializable extends Readable, ValueBox<String> {
    String toSerializedString();

    @Override
    default String getValue() {
        return toSerializedString();
    }

    @Override
    default ValueType<? extends String> getHeldType() {
        return StandardValueType.STRING;
    }

    @Override
    default Reader toReader() {
        return new StringReader(toSerializedString());
    }
}
