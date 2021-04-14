package org.comroid.api;

import java.io.Reader;
import java.io.StringReader;

public interface StringSerializable {
    String toSerializedString();

    default Reader toReader() {
        return new StringReader(toSerializedString());
    }
}
