package org.comroid.commands.model;

import java.util.stream.Stream;

@FunctionalInterface
public interface CommandContextProvider {
    Stream<?> expandContext(Object context);
}
