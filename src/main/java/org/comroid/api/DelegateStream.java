package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.comroid.util.Bitmask;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface DelegateStream extends Specifiable<AutoCloseable>, AutoCloseable {
    @MagicConstant(flagsFromClass = Capability.class)
    int getCapabilities();

    default InputStream input() {
        throw new AbstractMethodError(this + " does not implement InputStream");
    }

    default OutputStream output() {
        throw new AbstractMethodError(this + " does not implement OutputStream");
    }

    default OutputStream error() {
        throw new AbstractMethodError(this + " does not implement ErrorStream");
    }

    Stream<? extends AutoCloseable> getDependencies();

    boolean addDependency(AutoCloseable dependency);

    default <T extends DelegateStream> T plus(AutoCloseable dependency) throws ClassCastException {
        if (!addDependency(dependency))
            System.err.println("Could not add dependency " + dependency + " to " + this);
        //noinspection unchecked
        return (T) this;
    }

    @SafeVarargs
    private static <T extends AutoCloseable> Queue<T> collect(T[] array, T... prepend) {
        return Stream.concat(Stream.of(prepend), Stream.of(array)).sequential()
                .filter(Objects::nonNull)
                .collect(ArrayDeque::new, Collection::add, Collection::addAll);
    }

    enum Capability implements BitmaskAttribute<Capability> {Input, Output, Error}

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

        public Input(final Supplier<String> source, Closeable... dependencies) {
            this.read = new ThrowingIntSupplier<>() {
                @Nullable
                private String buf = null;
                private int i = -1;

                @Override
                public int getAsInt() {
                    if (buf == null || i + 1 >= buf.length()) {
                        buf = source.get();
                        i = 0;
                    }
                    return buf.charAt(++i);
                }
            };
            this.dependencies = collect(dependencies);
        }

        @Override
        public int read() throws IOException {
            return read.getAsInt();
        }

        @Override
        public int getCapabilities() {
            return Capability.Input.getAsInt();
        }

        @Override
        public InputStream input() {
            return this;
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

        public Output(final Consumer<String> handler, Closeable... dependencies) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.updateAndGet(buf -> {
                handler.accept(buf.toString());
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
        public int getCapabilities() {
            return Capability.Output.getAsInt();
        }

        @Override
        public OutputStream output() {
            return this;
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
    class IOE implements DelegateStream {
        public static final IOE SYSTEM = new IOE(System.in, System.out, System.err);

        private final Queue<Closeable> dependencies;
        public final Stack<DelegateStream> redirect = new Stack<>();
        @Nullable
        @Delegate(excludes = {Closeable.class})
        private final InputStream in;
        @Nullable
        @Delegate(excludes = {Closeable.class})
        private final OutputStream out;
        @Nullable
        private final OutputStream err;

        public static IOE slf4j(Logger log) {
            return new IOE(new Output(log, Level.INFO), new Output(log, Level.ERROR));
        }

        public IOE(Closeable... dependencies) {
            this.dependencies = collect(dependencies);
            in = new RedirectInput();
            out = new RedirectOutput(false);
            err = new RedirectOutput(true);
        }

        public IOE(@Nullable OutputStream out,  Closeable... dependencies) {
            this(out, null, dependencies);
        }

        public IOE(@Nullable OutputStream out, @Nullable OutputStream err, Closeable... dependencies) {
            this(null, out, err, dependencies);
        }

        public IOE(@Nullable InputStream in,  Closeable... dependencies) {
            this(in, null, dependencies);
        }

        public IOE(@Nullable InputStream in, @Nullable OutputStream out, Closeable... dependencies) {
            this(in, out, null, dependencies);
        }

        public IOE(@Nullable InputStream in, @Nullable OutputStream out, @Nullable OutputStream err, Closeable... dependencies) {
            this.dependencies = collect(dependencies, in, out, err);
            this.in = in;
            this.out = out;
            this.err = err;
        }

        @Override
        public int getCapabilities() {
            return Bitmask.arrange(in != null, out != null, err != null);
        }

        public boolean hasCapability(Capability capability) {
            return Bitmask.isFlagSet(getCapabilities(), capability);
        }

        @Override public InputStream input() {return Objects.requireNonNull(in,this + " does not contain an InputStream");}
        @Override public OutputStream output() {return Objects.requireNonNull(out,this + " does not contain an OutputStream");}
        @Override public OutputStream error() {return Objects.requireNonNull(err,this + " does not contain an ErrorStream");}

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
            redirect.clear();
            while (!dependencies.isEmpty())
                dependencies.poll().close();
        }

        private class RedirectInput extends InputStream {
            @Override
            public int read() throws IOException {
                for (var ioe : redirect) {
                    if (Bitmask.isFlagSet(ioe.getCapabilities(), Capability.Input)) {
                        return ioe.input().read();
                    }
                }
                return SYSTEM.input().read();
            }
        }

        private class RedirectOutput extends OutputStream {
            private final boolean err;
            private final Capability cap;

            public RedirectOutput(boolean error) {
                this.err = error;
                this.cap = error ? Capability.Error : Capability.Output;
            }

            @Override
            public void write(int b) throws IOException {
                if (redirect.empty())
                    $(SYSTEM).write(b);
                else for (var delegate : redirect) {
                    if (Bitmask.isFlagSet(delegate.getCapabilities(), cap))
                        $(delegate).write(b);
                }
            }

            @Override
            public void flush() throws IOException {
                if (redirect.empty())
                    $(SYSTEM).flush();
                else for (var delegate : redirect) {
                    if (Bitmask.isFlagSet(delegate.getCapabilities(), cap))
                        $(delegate).flush();
                }
            }

            private OutputStream $(DelegateStream delegate) {
                return (err ? delegate.error() : delegate.output());
            }
        }
    }
}
