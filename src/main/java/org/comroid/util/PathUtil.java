package org.comroid.util;

import org.comroid.api.os.OS;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class PathUtil {
    public static Optional<File> findExec(String name) {
        return Arrays.stream(System.getenv("PATH").split(File.pathSeparator))
                .map(p -> Path.of(p, name + (OS.isWindows ? ".exe" : "")).toFile())
                .filter(File::exists)
                .findAny();
    }

    public static String sanitize(Object string) {
        var str = string.toString().replace('\\', '/');
        for (var c : new char[]{ '<', '>', ':', '"', '|', '?', '*'/*,'/','\\'*/ })
            str = str.replace(c, '_');
        return str;
    }
}
