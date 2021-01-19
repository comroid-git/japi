package org.comroid.api;

public interface UncheckedCloseable extends AutoCloseable {
    @Override
    void close();
}
