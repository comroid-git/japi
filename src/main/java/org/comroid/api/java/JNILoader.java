package org.comroid.api.java;

import org.comroid.api.os.Architecture;
import org.comroid.api.os.OS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class JNILoader {
    public static void loadLibrary(String name) {
        loadLibrary(name + "-32", name + "-64");
    }

    public static void loadLibrary(String x32name, String x64name) {
        loadLibrary(null, x32name, x64name);
    }

    public static void loadLibrary(String path, String x32name, String x64name) {
        String useName = Architecture.current == Architecture.x64 ? x64name : x32name;
        try {
            System.loadLibrary((path == null ? "" : path + File.separator) + useName);
        } catch (UnsatisfiedLinkError ignored) {
            loadFromJar(useName);
        }
    }

    public static void loadFromJar(String name) {
        File fileOut = null;

        try {
            fileOut = File.createTempFile(name + '-', OS.current.getLibraryExtension());

            if (!fileOut.exists()) {
                if (!fileOut.createNewFile())
                    throw new RuntimeException("Could not create file " + fileOut);
                try (
                        InputStream in = ClassLoader.getSystemResourceAsStream(name);
                        OutputStream out = new FileOutputStream(fileOut, false)
                ) {
                    if (in == null)
                        throw new UnsatisfiedLinkError("No such library: " + name);

                    byte[] buf = new byte[8192];
                    int length;
                    while ((length = in.read(buf)) > 0)
                        out.write(buf, 0, length);
                }
            }
            System.load(fileOut.toString());
        } catch (Exception e) {
            throw new RuntimeException("Error loading library into file: "
                                               + (fileOut == null ? "null" : fileOut.getAbsolutePath()), e);
        }
    }

    private JNILoader() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
