package org.comroid.api;

import org.comroid.api.func.PropertyHolder;
import org.comroid.api.func.exc.ThrowingRunnable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public interface Disposable extends Closeable, PropertyHolder {
    @NonExtendable
    default void addChildren(Closeable... childs) {
        for (Closeable child : childs)
            getCloseables().add(child);
    }

    @NonExtendable
    default Set<? super Closeable> getCloseables() {
        //noinspection unchecked
        return ((Set<? super Closeable>) getPropertyCache().computeIfAbsent("disposable-children", key -> new HashSet<>()));
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
    default void disposeThrow() throws MultipleExceptions {
        final List<? extends Throwable> throwables = dispose();

        if (throwables.isEmpty()) {
            return;
        }

        throw new MultipleExceptions(throwables);
    }

    @NonExtendable
    default List<? extends Throwable> dispose() {
        return Collections.unmodifiableList(Stream.concat(
                        getCloseables().stream().map(Closeable.class::cast),
                        Stream.of(ThrowingRunnable.rethrowing(this::closeSelf, null)::run)
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

    final class MultipleExceptions extends RuntimeException {
        public MultipleExceptions(String message, Collection<? extends Throwable> causes) {
            super(composeMessage(message, causes));
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
            final StringStream out    = new StringStream();
            final PrintStream  string = new PrintStream(out);

            string.println(baseMessage);
            string.println("Sub Stacktraces in order:");
            throwables.forEach(t -> t.printStackTrace(string));

            return out.toString();
        }

        public MultipleExceptions(Collection<? extends Throwable> causes) {
            super(composeMessage(null, causes));
        }
    }
}
