package org.comroid.api.java;

import org.comroid.util.PathUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public interface ResourceLoader {
    ResourceLoader SYSTEM_CLASS_LOADER = ofClassLoader(ClassLoader.getSystemClassLoader());

    static ResourceLoader ofSystemClassLoader() {
        return ofClassLoader(ClassLoader.getSystemClassLoader());
    }

    static ResourceLoader ofClassLoader(final ClassLoader classLoader) {
        return classLoader::getResourceAsStream;
    }

    static ResourceLoader ofCallerClassLoader() {
        return ofClassLoader(StackTraceUtils.callerClass(1).getClassLoader());
    }

    static ResourceLoader ofDirectory(final File dir) {
        if (!dir.isDirectory())
            throw new IllegalArgumentException("File is not a directory: " + dir);
        if (!dir.exists())
            throw new IllegalArgumentException("Directory does not exist");
        return name -> {
            try {
                return new FileInputStream(new File(dir, name));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Resource not found: " + name, e);
            }
        };
    }

    static InputStream fromResourceString(String string) throws FileNotFoundException {
        if (string.startsWith("@:"))
            return ClassLoader.getSystemClassLoader()
                    .getResourceAsStream(PathUtil.sanitize(string.substring(2)));
        if (string.startsWith("@"))
            return new FileInputStream(PathUtil.sanitize(string.substring(1)));
        return new ByteArrayInputStream(PathUtil.sanitize(string).getBytes());
    }

    default Reader getResourceReader(String name) {
        return new InputStreamReader(getResource(name));
    }

    InputStream getResource(String name);
}
