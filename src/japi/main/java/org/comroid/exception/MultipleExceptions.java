package org.comroid.exception;

import lombok.Singular;
import lombok.experimental.StandardException;

import java.util.List;

@StandardException
public class MultipleExceptions extends RuntimeException {
    public @Singular List<Throwable> part;
}
