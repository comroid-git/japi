package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.comroid.util.Bitmask;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.comroid.util.StackTraceUtils.caller;

public interface DelegateStream extends Specifiable<AutoCloseable>, AutoCloseable, Named {
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
    private static <T extends AutoCloseable> Deque<T> collect(T[] array, T... prepend) {
        return Stream.concat(Stream.of(prepend), Stream.of(array)).sequential()
                .filter(Objects::nonNull)
                .collect(ArrayDeque::new, Collection::add, Collection::addAll);
    }

    enum Capability implements BitmaskAttribute<Capability> {Input, Output, Error}

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Input extends InputStream implements DelegateStream {
        private final ThrowingIntSupplier<IOException> read;
        private final Queue<Closeable> dependencies;
        private final String desc;

        public Input(final InputStream delegate, Closeable... dependencies) {
            this.read = delegate::read;
            this.dependencies = collect(dependencies, delegate);
            this.desc = String.format("Proxy InputStream from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        public Input(final Reader delegate, Closeable... dependencies) {
            this.read = delegate::read;
            this.dependencies = collect(dependencies, delegate);
            this.desc = String.format("Reader delegating InputStream from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        @Experimental
        public Input(final Supplier<String> source, Closeable... dependencies) {
            this.read = new ThrowingIntSupplier<>() {
                @Nullable
                private String buf = null;
                private int i = -1;

                @Override
                public int getAsInt() {
                    if (buf == null || i + 1 > buf.length()) {
                        buf = source.get();
                        i = 0;
                    } else if (++i == buf.length())
                        return -1;
                    return buf.charAt(i);
                }
            };
            this.dependencies = collect(dependencies);
            this.desc = String.format("Adapter InputStream from %s with %d dependencies", caller(1), this.dependencies.size());
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

        @Override
        public String getName() {
            return desc;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Output extends OutputStream implements DelegateStream {
        private final String desc;
        private final ThrowingIntConsumer<IOException> write;
        private final ThrowingRunnable<IOException> flush;
        private final Queue<Closeable> dependencies;
        private StringBuilder buf = new StringBuilder();

        public Output(final OutputStream delegate, Closeable... dependencies) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.dependencies = collect(dependencies, delegate);
            this.desc = String.format("Proxy OutputStream from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        public Output(final Writer delegate, Closeable... dependencies) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.dependencies = collect(dependencies, delegate);
            this.desc = String.format("Writer delegating OutputStream from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        public Output(final Logger log, final Level level, Closeable... dependencies) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.updateAndGet(buf -> {
                log.atLevel(level).log(buf.toString());
                return new StringWriter();
            });
            this.dependencies = collect(dependencies);
            this.desc = String.format("Log delegating OutputStream from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        public Output(final Consumer<String> handler, Closeable... dependencies) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.getAndUpdate(buf -> {
                handler.accept(buf.toString());
                return new StringWriter();
            });
            this.dependencies = collect(dependencies);
            this.desc = String.format("Adapter OutputStream from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        @Override
        public void write(int b) throws IOException {
            write.accept(b);
            buf.append((char)b);
        }

        @Override
        public void flush() throws IOException {
            flush.run();
            var $ = buf.toString();
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

        @Override
        public String getName() {
            return desc;
        }
    }

    @Slf4j
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IO implements DelegateStream {
        public static final IO SYSTEM = new IO(System.in, System.out, System.err);

        private final String desc;
        private final Deque<Closeable> dependencies;
        public final Deque<DelegateStream> redirect = new ArrayDeque<>();
        @Nullable
        @Delegate(excludes = {Closeable.class})
        private final InputStream in;
        @Nullable
        @Delegate(excludes = {Closeable.class})
        private final OutputStream out;
        @Nullable
        private final OutputStream err;

        public static IO slf4j(Logger log) {
            return new IO(new Output(log, Level.INFO), new Output(log, Level.ERROR));
        }

        public IO(Closeable... dependencies) {
            this(Integer.MAX_VALUE, dependencies);
        }

        public IO(int capabilities, Closeable... dependencies) {
            this.dependencies = collect(dependencies);
            in = Bitmask.isFlagSet(capabilities, Capability.Input) ? new RedirectInput() : null;
            out = Bitmask.isFlagSet(capabilities, Capability.Output) ? new RedirectOutput(false) : null;
            err = Bitmask.isFlagSet(capabilities, Capability.Error) ? new RedirectOutput(true) : null;
            this.desc = String.format("Redirect-IO from %s with %d dependencies", caller(1), this.dependencies.size());
        }

        public IO(@Nullable OutputStream out, Closeable... dependencies) {
            this(out, null, dependencies);
        }

        public IO(@Nullable OutputStream out, @Nullable OutputStream err, Closeable... dependencies) {
            this(null, out, err, dependencies);
        }

        public IO(@Nullable InputStream in, Closeable... dependencies) {
            this(in, null, dependencies);
        }

        public IO(@Nullable InputStream in, @Nullable OutputStream out, Closeable... dependencies) {
            this(in, out, null, dependencies);
        }

        public IO(@Nullable InputStream in, @Nullable OutputStream out, @Nullable OutputStream err, Closeable... dependencies) {
            this.dependencies = collect(dependencies, in, out, err);
            this.in = in;
            this.out = out;
            this.err = err;
            this.desc = String.format("Container-IO from %s with %d dependencies", caller(1), this.dependencies.size());
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

        @Override
        public String getName() {
            return desc;
        }

        public String getAlternativeName() {return toInfoString(1);}
        private String toInfoString(int indent) {
            var sb = new StringBuilder();

            String here = '\n'+"|\t".repeat(indent)+"|>";
            String tabs = '\n'+"|\t".repeat(indent)+"|-- ";

            sb.append(desc).append(here);
            if (redirect.size() == 0)
                sb.append("No Redirects");
            else sb.append("Redirects:");
            for (var redir : redirect) {
                if (redir instanceof IO)
                    sb.append(tabs).append(((IO) redir).toInfoString(indent+1));
                else sb.append(tabs).append(redir);
            }

            sb.append(here);
            if (dependencies.size() == 0)
                sb.append("No dependencies");
            else sb.append("Dependencies:");
            for (var dep : dependencies) {
                if (dep instanceof IO)
                    sb.append(tabs).append(((IO) dep).toInfoString(indent+1));
                else sb.append(tabs).append(dep);
            }

            sb.append(here).append("input(): ").append(ThrowingSupplier.fallback(this::input).get());
            sb.append(here).append("output(): ").append(ThrowingSupplier.fallback(this::output).get());
            sb.append(here).append("error(): ").append(ThrowingSupplier.fallback(this::error).get());

            return sb.toString();
        }

        private class RedirectInput extends InputStream {
            @Override
            public int read() {
                try {
                    for (var ioe : redirect) {
                        if (Bitmask.isFlagSet(ioe.getCapabilities(), Capability.Input)) {
                            return ioe.input().read();
                        }
                    }
                    return SYSTEM.input().read();
                } catch (Throwable t) {
                    log.error("Error reading from InputStream", t);
                    return -1;
                }
            }

            @Override
            public String toString() {
                return "RedirectInput of " + desc;
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
            public void write(int b) {
                try {
                    if (redirect.isEmpty())
                        $(SYSTEM).write(b);
                    else for (var delegate : redirect) {
                        if (Bitmask.isFlagSet(delegate.getCapabilities(), cap))
                            $(delegate).write(b);
                    }
                } catch (Throwable t) {
                    log.error("Error writing to Output", t);
                }
            }

            @Override
            public void flush() {
                try {
                    if (redirect.isEmpty())
                        $(SYSTEM).flush();
                    else for (var delegate : redirect) {
                        if (Bitmask.isFlagSet(delegate.getCapabilities(), cap))
                            $(delegate).flush();
                    }
                } catch (Throwable t) {
                    log.error("Error flushing Output", t);
                }
            }

            private OutputStream $(DelegateStream delegate) {
                return (err ? delegate.error() : delegate.output());
            }

            @Override
            public String toString() {
                return "RedirectOutput of " + desc;
            }
        }
    }
}
