package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.comroid.util.Bitmask;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.util.StackTraceUtils.caller;

public interface DelegateStream extends Specifiable<Closeable>, Closeable, Named {
    AutoCloseable getDelegate();

    @MagicConstant(flagsFromClass = Capability.class)
    int getCapabilities();

    default InputStream input() {
        throw unsupported(this, Capability.Input);
    }

    default OutputStream output() {
        throw unsupported(this, Capability.Output);
    }

    default OutputStream error() {
        throw unsupported(this, Capability.Error);
    }

    private static UnsupportedOperationException unsupported(DelegateStream stream, Capability capability) {
        return new UnsupportedOperationException(String.format("%s has no support for %s", stream, capability));
    }

    @SafeVarargs
    private static <T extends Closeable> Deque<T> collect(T[] array, T... prepend) {
        return Stream.concat(Stream.of(prepend), Stream.of(array)).sequential()
                .filter(Objects::nonNull)
                .collect(ArrayDeque::new, Collection::add, Collection::addAll);
    }

    enum Capability implements BitmaskAttribute<Capability> {Input, Output, Error}

    @Value
    @Slf4j
    @EqualsAndHashCode(callSuper = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Input extends InputStream implements DelegateStream {
        ThrowingIntSupplier<IOException> read;
        @Getter AutoCloseable delegate;
        String desc;

        public Input(final InputStream delegate) {
            this.read = delegate::read;
            this.delegate = delegate;
            this.desc = "Proxy InputStream @ " + caller(1);
        }

        public Input(final Reader delegate) {
            this.read = delegate::read;
            this.delegate = delegate;
            this.desc = "Reader delegating InputStream @ " + caller(1);
        }

        @ApiStatus.Experimental
        public Input(final Event.Bus<String> source) {
            final var queue = new LinkedBlockingQueue<String>();
            var listener = source.listen(e -> {
                synchronized (queue) {
                    if (e.test(queue::add))
                        queue.notify();
                    else log.error("Failed to queue new input " + e);
                }
            });
            this.read = new ThrowingIntSupplier<>() {
                @Nullable
                private String buf = null;
                private int i = -1;
                private boolean eof = false;

                @Override
                @SneakyThrows
                public int getAsInt() {
                    if (eof) {
                        eof = false;
                        return -1;
                    }
                    if (buf == null || i + 1 > buf.length()) {
                        synchronized (queue) {
                            while (queue.isEmpty())
                                queue.wait();
                        }
                        buf = queue.poll();
                        i = 0;
                    } else if (++i == buf.length()) {
                        buf = null;
                        eof = true;
                        return '\n';
                    }
                    return buf.charAt(i);
                }
            };
            this.delegate = source;
            this.desc = "Adapter InputStream @ " + caller(1);
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
        @SneakyThrows
        public void close() {
            delegate.close();
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    class Output extends OutputStream implements DelegateStream {
        ThrowingIntConsumer<IOException> write;
        ThrowingRunnable<IOException> flush;
        @Getter AutoCloseable delegate;
        String desc;
        StringBuilder buf = new StringBuilder();

        public Output(final OutputStream delegate) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.delegate = delegate;
            this.desc = "Proxy OutputStream @ " + caller(1);
        }

        public Output(final Writer delegate) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.delegate = delegate;
            this.desc = "Writer delegating OutputStream @ " + caller(1);
        }

        public Output(final Logger log, final Level level) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.updateAndGet(buf -> {
                log.atLevel(level).log(buf.toString());
                return new StringWriter();
            });
            this.delegate = ()->{};
            this.desc = "Log delegating OutputStream @ " + caller(1);
        }

        public Output(final Consumer<String> handler) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.getAndUpdate(buf -> {
                handler.accept(buf.toString());
                return new StringWriter();
            });
            this.delegate = ()->{};
            this.desc = "Adapter OutputStream @ " + caller(1);
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
        @SneakyThrows
        public void close() {
            delegate.close();
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    @Value
    @Slf4j
    class IO implements DelegateStream {
        public static final IO SYSTEM = new IO(System.in, System.out, System.err);

        int initialCapabilities;
        public Deque<DelegateStream> redirects;
        @Nullable InputStream in;
        @Nullable OutputStream out;
        @Nullable OutputStream err;
        @Getter AutoCloseable delegate;
        String desc;

        public static IO slf4j(Logger log) {
            return new IO(new Output(log, Level.INFO), new Output(log, Level.ERROR));
        }

        public static IO with(Capability... capabilities) {
            var builder = builder();
            for (Capability capability : capabilities)
                builder.initialCapability(capability);
            return builder.build();
        }

        public IO() {
            this(null,null);
        }

        public IO(@Nullable OutputStream out, @Nullable OutputStream err) {
            this(null, out, err);
        }

        public IO(@Nullable InputStream in, @Nullable OutputStream out, @Nullable OutputStream err) {
            this(BitmaskAttribute.valueOf(Bitmask.arrange(in==null,out==null,err==null),Capability.values()), in,out,err, List.of());
        }

        @lombok.Builder(builderClassName = "Builder")
        private IO(
                @Singular Set<Capability> initialCapabilities,
                @Nullable InputStream in,
                @Nullable OutputStream out,
                @Nullable OutputStream err,
                @Singular List<DelegateStream> redirects
        ) {
            this.initialCapabilities = Bitmask.combine(initialCapabilities.toArray(Capability[]::new));
            this.redirects = DelegateStream.collect(redirects.toArray(DelegateStream[]::new));
            this.in = obtainStream(in,Capability.Input,()->new RedirectInput());
            this.out = obtainStream(out,Capability.Output,()->new RedirectOutput(Capability.Output));
            this.err = obtainStream(err,Capability.Error,()->new RedirectOutput(Capability.Error));
            this.delegate = ()->{
                if (this.in!=null)this.in.close();
                if (this.out!=null)this.out.close();
                if (this.err!=null)this.err.close();
            };
            this.desc = "IO @ " + caller(2);
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
        public String toString() {
            return desc;
        }
        public String getAlternateName() {return toInfoString(1);}

        private String toInfoString(int indent) {
            var sb = new StringBuilder(getName());

            String here = '\n'+"|\t".repeat(indent-1)+"╠═╦> ";
            String tabs = '\n'+"|\t".repeat(indent-1)+"│ ├> ";
            String desc = '\n'+"|\t".repeat(indent-1)+"│ ├─ ";

            if (redirects.size() != 0) {
                sb.append(here).append("Redirects:");
                for (var redirect : redirects) {
                    sb.append(tabs);
                    if (redirect instanceof IO)
                        sb.append(((IO) redirect).toInfoString(indent+1));
                    else {
                        sb.append(redirect);
                        if (redirect.getDelegate() != null)
                            sb.append(desc).append(redirect.getDelegate());
                    }
                }
            }

            if (this.desc.startsWith("IO"))
                for (var capability : Capability.values()) {
                    var list = stream(capability).collect(Collectors.toList());
                    if (list.isEmpty()) {
                        continue;
                    }
                    sb.append(here).append(capability.getName()).append(":");
                    stream(capability).forEach(tgt -> {
                        var it = tgt.toString();
                        sb.append(tabs).append(it);
                        Optional.of(tgt)
                                .filter(DelegateStream.class::isInstance)
                                .map(DelegateStream.class::cast)
                                .map(Objects::toString)
                                .filter(Predicate.not(it::equals))
                                .ifPresent(delegate -> sb.append(desc).append(delegate));
                    });
                }

            return sb.toString();
        }
        private <R> R obtainStream(@Nullable R input, final Capability capability, final Supplier<R> fallback) {
            return Optional.ofNullable(input).orElseGet(() -> Bitmask
                    .isFlagSet(this.initialCapabilities, capability) ? fallback.get() : null);
        }

        @Override
        @SneakyThrows
        public void close() {
            delegate.close();
        }

        private int runOnInput(ThrowingToIntFunction<InputStream, ? extends Throwable> action) {
            return this.<InputStream>stream(Capability.Input)
                    .mapToInt(action.wrap())
                    .findFirst()
                    .orElseThrow(() -> unsupported(this, Capability.Input));
        }
        private void runOnOutput(Capability capability, ThrowingConsumer<OutputStream, ? extends Throwable> action) {
            this.<OutputStream>stream(capability).forEachOrdered(action.wrap(null));
        }
        private <T> Stream<T> stream(final Capability capability) {
            return redirects.stream() // Stream.concat(redirect.stream(), Stream.of(SYSTEM))
                    .filter(it->Bitmask.isFlagSet(it.getCapabilities(), capability))
                    .map(it->{switch(capability){
                        case Input: return it.input();
                        case Output: return it.output();
                        case Error: return it.error();}
                        return null;})
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(Polyfill::uncheckedCast);
        }

        private class RedirectInput extends InputStream {
            @Override
            public int read() {
                try {
                    return runOnInput(InputStream::read);
                } catch (Throwable t) {
                    log.error("Error reading from InputStream", t);
                    return -1;
                }
            }

            @Override
            public String toString() {
                return "RedirectInput of " + getName();
            }
        }

        private class RedirectOutput extends OutputStream {
            private final Capability capability;

            public RedirectOutput(Capability capability) {
                this.capability = capability;
            }

            @Override
            public void write(final int b) {
                try {
                    runOnOutput(capability, s->s.write(b));
                } catch (Throwable t) {
                    log.error("Error writing to Output", t);
                }
            }

            @Override
            public void flush() {
                try {
                    runOnOutput(capability, OutputStream::flush);
                } catch (Throwable t) {
                    log.error("Error flushing Output", t);
                }
            }

            private OutputStream $(DelegateStream delegate) {
                return (capability == Capability.Error ? delegate.error() : delegate.output());
            }

            @Override
            public String toString() {
                return "RedirectOutput of " + getName();
            }
        }
    }
}
