package org.comroid.exception;

import org.comroid.api.attr.Named;

public class BlankException extends RuntimeException implements Named {
    private final String name;

    public BlankException(String name, String format, Object... args) {
        this(name, String.format(format, args));
    }

    public BlankException(String name, String message) {
        super(message);
        this.name = name;
    }

    public BlankException(String name, Throwable cause, String format, Object... args) {
        this(name, String.format(format, args), cause);
    }

    public BlankException(String name, String message, Throwable cause) {
        super(message, cause);
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", name, getMessage());
    }
}
