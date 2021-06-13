package org.comroid.api;

import java.io.Reader;
import java.io.StringReader;

public interface StringSerializable extends Readable {
    String toSerializedString();

    @Override
    default Reader toReader() {
        return new StringReader(toSerializedString());
    }
}
