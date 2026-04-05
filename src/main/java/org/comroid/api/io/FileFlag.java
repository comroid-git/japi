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
        return cache.containsKey(file) && cache.get(file).enable();
    }

    public static boolean consume(File file) {
        return cache.containsKey(file) && cache.get(file).consume();
    }

    File file;

    @SneakyThrows
    public boolean enable() {
        if (!file.isAbsolute()) return enable(file.getAbsoluteFile());
        return file.exists() || file.createNewFile();
    }

    public boolean consume() {
        if (!file.isAbsolute()) return consume(file.getAbsoluteFile());
        return file.exists() && file.delete();
    }
}
