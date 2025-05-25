package org.comroid.api.io;

import java.io.File;

public final class FileFlag {
    public static boolean consume(File file) {
        if (!file.isAbsolute())
            return consume(file.getAbsoluteFile());
        return file.exists() && file.delete();
    }

    private FileFlag() {
        throw new UnsupportedOperationException();
    }
}
