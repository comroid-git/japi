package org.comroid.api.os;

import org.comroid.api.Named;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public enum OS implements Named {
    WINDOWS(".dll", "win"),
    MAC(".so", "mac"),
    UNIX(".so", "nix", "nux", "aix"),
    SOLARIS(".so", "sunos");

    public static final OS current;
    public static final boolean isWindows;
    public static final boolean isMac;
    public static final boolean isUnix;
    public static final boolean isSolaris;

    static {
        current = detect();
        isWindows = current == WINDOWS;
        isMac = current == MAC;
        isUnix = current == UNIX;
        isSolaris = current == SOLARIS;
    }

    private final String libExtension;
    private final List<String> validators;

    public String getLibraryExtension() {
        return libExtension;
    }

    OS(String libExtension, String... validators) {
        this.libExtension = libExtension;
        this.validators = Collections.unmodifiableList(Arrays.asList(validators));
    }

    private static OS detect() {
        if (current != null)
            return current;

        String osName = System.getProperty("os.name");
        return findByName(osName);
    }

    public @NotNull
    static OS findByName(String osName) {
        for (OS value : values()) {
            final String osLow = osName.toLowerCase();
            if (value.validators.stream().anyMatch(osLow::contains))
                return value;
        }

        throw new NoSuchElementException("Unknown OS: " + osName);
    }

}
