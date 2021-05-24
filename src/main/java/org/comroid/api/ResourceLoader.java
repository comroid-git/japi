package org.comroid.api;

import java.io.*;

public interface ResourceLoader {
    static ResourceLoader ofClassLoader(final ClassLoader classLoader) {
        return classLoader::getResourceAsStream;
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

    InputStream getResource(String name);

    default Reader getResourceReader(String name) {
        return new InputStreamReader(getResource(name));
    }
}
