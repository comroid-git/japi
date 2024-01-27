package org.comroid.api.data.seri;

import org.comroid.api.data.Readable;

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

public interface StringSerializable extends Readable, Serializable {
    String toSerializedString();

    @Override
    default Reader toReader() {
        return new StringReader(toSerializedString());
    }
}
