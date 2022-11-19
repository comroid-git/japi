package org.comroid.api.io;

import org.comroid.api.UncheckedCloseable;
import org.comroid.api.Disposable;

import java.io.IOException;
import java.util.UUID;

public interface FileProcessor extends Disposable, UncheckedCloseable {
    FileHandle getFile();

    int storeData() throws IOException;

    int reloadData() throws IOException;

    @Override
    default void close() throws MultipleExceptions {
        try {
            storeData();
        } catch (IOException e) {
            throw new RuntimeException("Could not store data", e);
        } finally {
            disposeThrow();
        }
    }

    interface Underlying extends FileProcessor {
        FileProcessor getUnderlyingFileProcessor();

        @Override
        default FileHandle getFile() {
            return getUnderlyingFileProcessor().getFile();
        }

        @Override
        default UUID getUUID() {
            return getUnderlyingFileProcessor().getUUID();
        }

        @Override
        default int storeData() throws IOException {
            return getUnderlyingFileProcessor().storeData();
        }

        @Override
        default int reloadData() throws IOException {
            return getUnderlyingFileProcessor().reloadData();
        }
    }
}
