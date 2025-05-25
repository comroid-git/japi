package org.comroid.api.io;

import lombok.SneakyThrows;

import java.io.File;

public final class FileFlag {
    @SneakyThrows
    public static boolean enable(File file) {
        if (!file.isAbsolute()) return enable(file.getAbsoluteFile());
        return file.exists() || file.createNewFile();
    }

    public static boolean consume(File file) {
        if (!file.isAbsolute()) return consume(file.getAbsoluteFile());
        return file.exists() && file.delete();
    }

    private FileFlag() {
        throw new UnsupportedOperationException();
    }
}
