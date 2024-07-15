package org.comroid.api.io;

import org.comroid.api.Disposable;
import org.comroid.api.tree.UncheckedCloseable;

import java.io.IOException;
import java.util.UUID;

public interface FileProcessor extends Disposable, UncheckedCloseable {
    FileHandle getFile();

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

    int storeData() throws IOException;

    interface Underlying extends FileProcessor {
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

        FileProcessor getUnderlyingFileProcessor();

        @Override
        default int reloadData() throws IOException {
            return getUnderlyingFileProcessor().reloadData();
        }
    }
}
