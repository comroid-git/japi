package org.comroid.api.tree;

import java.io.Closeable;

public interface UncheckedCloseable extends Closeable {
    @Override
    void close();
}
