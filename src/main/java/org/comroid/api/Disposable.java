package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Disposable extends AutoCloseable, PropertyHolder {
    @NonExtendable
    default Set<? super AutoCloseable> getCloseables() {
        //noinspection unchecked
        return ((Set<? super AutoCloseable>) getPropertyCache().computeIfAbsent("disposable-children", key -> new HashSet<>()));
    }

    @NonExtendable
    default void addChildren(AutoCloseable... childs) {
        for (AutoCloseable child : childs)
            getCloseables().add(child);
    }

    @OverrideOnly
    @SuppressWarnings("RedundantThrows")
    default void closeSelf() throws Exception {
    }

    @Override
    @NonExtendable
    default void close() throws MultipleExceptions {
        disposeThrow();
    }

    @NonExtendable
    default List<? extends Throwable> dispose() {
        return Collections.unmodifiableList(Stream.concat(
                        getCloseables().stream().map(AutoCloseable.class::cast),
                        Stream.of(this::closeSelf)
                )
                .map(closeable -> {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        return e;
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @NonExtendable
    default void disposeThrow() throws MultipleExceptions {
        final List<? extends Throwable> throwables = dispose();

        if (throwables.isEmpty()) {
            return;
        }

        throw new MultipleExceptions(throwables);
    }

    final class MultipleExceptions extends RuntimeException {
        public MultipleExceptions(String message, Collection<? extends Throwable> causes) {
            super(composeMessage(message, causes));
        }

        public MultipleExceptions(Collection<? extends Throwable> causes) {
            super(composeMessage(null, causes));
        }

        private static String composeMessage(
                @Nullable String baseMessage, Collection<? extends Throwable> throwables
        ) {
            class StringStream extends OutputStream {
                private final StringBuilder sb = new StringBuilder();

                @Override
                public void write(int b) {
                    sb.append((char) b);
                }

                @Override
                public String toString() {
                    return sb.toString();
                }
            }

            if (baseMessage == null) {
                baseMessage = "Multiple Exceptions were thrown";
            }
            final StringStream out = new StringStream();
            final PrintStream string = new PrintStream(out);

            string.println(baseMessage);
            string.println("Sub Stacktraces in order:");
            throwables.forEach(t -> t.printStackTrace(string));

            return out.toString();
        }
    }
}
