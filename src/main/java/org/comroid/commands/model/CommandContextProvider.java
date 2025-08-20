package org.comroid.commands.model;

import java.util.stream.Stream;

@FunctionalInterface
public interface CommandContextProvider {
    Stream<Object> expandContext(Object... context);
}
