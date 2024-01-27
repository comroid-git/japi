package org.comroid.api.os;

import org.comroid.api.attr.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public enum Architecture implements Named {
    x32("32", "86"), x64("64");

    public static final Architecture current = Architecture.detect();

    private final List<String> idents;

    Architecture(String... idents) {
        this.idents = Collections.unmodifiableList(Arrays.asList(idents));
    }

    private static Architecture detect() {
        String arch = System.getProperty("os.arch");
        return Arrays.stream(values())
                .filter(it -> it.idents.stream().anyMatch(arch::contains))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Unknown architecture: " + arch));
    }
}
