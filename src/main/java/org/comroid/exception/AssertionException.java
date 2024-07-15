package org.comroid.exception;

import java.util.Objects;
import java.util.function.BiPredicate;

@Deprecated
public class AssertionException extends RuntimeException {
    public static <X, Y> boolean expect(X expected, Y actual) throws AssertionException {
        return expect(expected, actual, "");
    }

    public static <X, Y> boolean expect(X expected, Y actual, String detail) throws AssertionException {
        return expect(expected, actual, Objects::equals, detail);
    }

    public static <X, Y> boolean expect(X expected, Y actual, BiPredicate<X, Y> condition, String detail) throws AssertionException {
        if (!condition.test(expected, actual))
            throw new AssertionException(expected, actual, detail);

        return true;
    }

    public static <X, Y> boolean expect(X expected, Y actual, BiPredicate<X, Y> condition) throws AssertionException {
        return expect(expected, actual, condition, "");
    }

    public AssertionException() {
    }

    public AssertionException(Throwable cause) {
        super(cause);
    }

    public AssertionException(String message, Throwable cause) {
        super(message, cause);
    }

    private AssertionException(Object expected, Object actual, String detail) {
        this(String.format("Invalid data: expected %s%s, found %s", detail.isEmpty() ? "" : (detail + " == "), expected, actual));
    }

    public AssertionException(String message) {
        super(message);
    }
}
