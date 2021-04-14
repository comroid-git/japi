package org.comroid.api;

import java.io.StringReader;

public interface StringSerializable {
    String toSerializedString();

    default StringReader toReader() {
        return new StringReader(toSerializedString());
    }
}
