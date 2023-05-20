package org.comroid.api;

import java.io.Closeable;

public interface UncheckedCloseable extends Closeable {
    @Override
    void close();
}
