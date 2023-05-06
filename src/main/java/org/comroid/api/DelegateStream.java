package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public interface DelegateStream extends Specifiable<AutoCloseable>, AutoCloseable {
    @SafeVarargs
    private static <T extends AutoCloseable> Queue<T> collect(T[] array, T... prepend) {
        return Stream.concat(Stream.of(prepend), Stream.of(array)).sequential()
                .filter(Objects::nonNull)
                .collect(ArrayDeque::new, Collection::add, Collection::addAll);
    }

    Stream<? extends AutoCloseable> getDependencies();

    boolean addDependency(AutoCloseable dependency);

    default <T extends DelegateStream> T plus(AutoCloseable dependency) throws ClassCastException {
        if (!addDependency(dependency))
            System.err.println("Could not add dependency " + dependency + " to " + this);
        //noinspection unchecked
        return (T) this;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Input extends InputStream implements DelegateStream {
        private final ThrowingIntSupplier<IOException> read;
        private final Queue<Closeable> dependencies;

        public Input(final InputStream delegate, Closeable... dependencies) {
            this.read = delegate::read;
            this.dependencies = collect(dependencies, delegate);
        }

        public Input(final Reader delegate, Closeable... dependencies) {
            this.read = delegate::read;
            this.dependencies = collect(dependencies, delegate);
        }

        @Override
        public int read() throws IOException {
            return read.getAsInt();
        }

        @Override
        public Stream<? extends AutoCloseable> getDependencies() {
            return dependencies.stream().filter(Objects::nonNull);
        }

        @Override
        public boolean addDependency(AutoCloseable dependency) {
            return dependency instanceof Closeable && dependencies.add((Closeable) dependency);
        }

        @Override
        @SneakyThrows
        public void close() {
            while (!dependencies.isEmpty())
                dependencies.poll().close();
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Output extends OutputStream implements DelegateStream {
        private final ThrowingIntConsumer<IOException> write;
        private final ThrowingRunnable<IOException> flush;
        private final Queue<Closeable> dependencies;
        @Nullable
        private @With String prefix;

        public Output(final OutputStream delegate, Closeable... dependencies) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.dependencies = collect(dependencies, delegate);
        }

        public Output(final Writer delegate, Closeable... dependencies) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.dependencies = collect(dependencies, delegate);
        }

        public Output(final Logger log, final Level level, Closeable... dependencies) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.updateAndGet(buf -> {
                log.atLevel(level).log(Objects.requireNonNullElse(prefix, "") + buf.toString());
                return new StringWriter();
            });
            this.dependencies = collect(dependencies);
        }

        @Override
        public void write(int b) throws IOException {
            write.accept(b);
        }

        @Override
        public void flush() throws IOException {
            flush.run();
        }

        @Override
        public Stream<? extends AutoCloseable> getDependencies() {
            return dependencies.stream().filter(Objects::nonNull);
        }

        @Override
        public boolean addDependency(AutoCloseable dependency) {
            return dependency instanceof Closeable && dependencies.add((Closeable) dependency);
        }

        @Override
        @SneakyThrows
        public void close() {
            while (!dependencies.isEmpty())
                dependencies.poll().close();
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IO implements DelegateStream {
        private final Queue<Closeable> dependencies;
        @Getter
        @Delegate(excludes = {Closeable.class})
        private final InputStream in;
        @Getter
        @Delegate(excludes = {Closeable.class})
        private final OutputStream out;

        public IO(InputStream in, OutputStream out, Closeable... dependencies) {
            this(in, out, null, dependencies);
        }

        private IO(InputStream in, OutputStream out, OutputStream err, Closeable[] dependencies) {
            this.in = in;
            this.out = out;
            this.dependencies = collect(dependencies, in, out, err);
        }

        @Override
        public Stream<? extends AutoCloseable> getDependencies() {
            return dependencies.stream().filter(Objects::nonNull);
        }

        @Override
        public boolean addDependency(AutoCloseable dependency) {
            return dependency instanceof Closeable && dependencies.add((Closeable) dependency);
        }

        @Override
        @SneakyThrows
        public void close() {
            while (!dependencies.isEmpty())
                dependencies.poll().close();
        }
    }

    class IOE extends IO {
        @Getter
        @Nullable
        private final OutputStream err;

        public IOE(InputStream in, OutputStream out, @Nullable OutputStream err, Closeable... dependencies) {
            super(in, out, dependencies);

            this.err = err;
        }
    }
}
