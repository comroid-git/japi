package org.comroid.api.io;

import lombok.SneakyThrows;
import lombok.Value;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class FileFlag {
    private static final Map<File, FileFlag> cache = new ConcurrentHashMap<>();

    @SneakyThrows
    public static boolean enable(File file) {
        if (!file.isAbsolute()) file = file.getAbsoluteFile();
        return cache.containsKey(file) && cache.get(file).enable();
    }

    public static boolean consume(File file) {
        if (!file.isAbsolute()) file = file.getAbsoluteFile();
        return cache.containsKey(file) && cache.get(file).consume();
    }

    File file;

    public FileFlag(File file) {
        this.file = file.getAbsoluteFile();
    }

    @SneakyThrows
    public boolean enable() {
        return file.exists() || file.createNewFile();
    }

    public boolean consume() {
        return file.exists() && file.delete();
    }
}
