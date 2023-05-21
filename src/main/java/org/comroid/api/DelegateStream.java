package org.comroid.api;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.comroid.api.info.Log;
import org.comroid.util.Bitmask;
import org.comroid.util.StackTraceUtils;
import org.comroid.util.StreamUtil;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface DelegateStream extends Container, Closeable, Named {
    AutoCloseable getDelegate();

    @MagicConstant(flagsFromClass = Capability.class)
    default int getCapabilities() {
        return 0;
    }

    default Rewrapper<InputStream> input() {return Rewrapper.empty();}
    default Rewrapper<OutputStream> output() {return Rewrapper.empty();}
    default Rewrapper<OutputStream> error() {return Rewrapper.empty();}

    static Input wrap(final InputStream stream) {
        return wrap(stream, Log.get());
    }

    static Input wrap(final InputStream stream, final Logger logger) {
        @Value
        @EqualsAndHashCode(callSuper = true)
        class WrapInputStream extends InputStream {
            @NonNull InputStream delegate;
            @NonNull Logger log;

            @Override
            public int read() {
                try {
                    return delegate.read();
                } catch (Throwable t) {
                    log.error("Could not read from " + delegate, t);
                    return -1;
                }
            }

            @Override
            public void close() {
                try {
                    delegate.close();
                } catch (Throwable t) {
                    log.error("Could not close " + delegate, t);
                }
            }
        }
        return new Input(new WrapInputStream(stream, logger));
    }

    static Output wrap(final OutputStream stream) {
        return wrap(stream, Log.get());
    }

    static Output wrap(final OutputStream stream, final Logger logger) {
        @Value
        @EqualsAndHashCode(callSuper = true)
        class WrapOutputStream extends OutputStream {
            @NonNull OutputStream delegate;
            @NonNull Logger log;

            @Override
            public void write(int b) {
                try {
                    delegate.write(b);
                } catch (Throwable t) {
                    log.error("Could not write to " + delegate, t);
                }
            }

            @Override
            public void flush() {
                try {
                    delegate.flush();
                } catch (Throwable t) {
                    log.error("Could not flush " + delegate, t);
                }
            }

            @Override
            public void close() {
                try {
                    delegate.close();
                } catch (Throwable t) {
                    log.error("Could not close " + delegate, t);
                }
            }
        }
        return new Output(new WrapOutputStream(stream, logger));
    }

    private static UnsupportedOperationException unsupported(DelegateStream stream, Capability capability) {
        return new UnsupportedOperationException(String.format("%s has no support for %s", stream, capability));
    }

    private static StackTraceElement caller() {return StackTraceUtils.caller(1);}

    enum Capability implements BitmaskAttribute<Capability> {Input, Output, Error}
    enum EndlMode implements IntegerAttribute {Manual, OnNewLine, OnDelegate}

    @Slf4j
    @Value
    @EqualsAndHashCode(callSuper = true)
    class Input extends InputStream implements DelegateStream {
        @lombok.experimental.Delegate(excludes = SelfCloseable.class)
        Container.Delegate<Input> container = new Delegate<>(this);
        @NonFinal @NonNull @Setter String name;
        ThrowingIntSupplier<IOException> read;
        @Nullable AutoCloseable delegate;

        public Input(final InputStream delegate) {
            this.read = delegate::read;
            this.delegate = delegate;
            this.name = "Proxy InputStream @ " + caller();
        }

        public Input(final Reader delegate) {
            this.read = delegate::read;
            this.delegate = delegate;
            this.name = "Reader InputStream @ " + caller();
        }

        @ApiStatus.Experimental
        public Input(final Event.Bus<String> source) {
            this(source, EndlMode.OnDelegate);
        }

        @ApiStatus.Experimental
        public Input(final Event.Bus<String> source, final EndlMode endlMode) {
            class EventBusHandler
                    extends Container.Base
                    implements Consumer<Event<String>>, ThrowingIntSupplier<IOException> {
                private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
                @Nullable
                private String buf = null;
                private final AtomicInteger i = new AtomicInteger(-1);
                private final AtomicInteger c = new AtomicInteger(0);
                private boolean endl = false;
                private boolean pad = true;

                @Override
                public void accept(Event<String> event) {
                    synchronized (queue) {
                        int c = this.c.incrementAndGet();
                        if (c != event.getSeq())
                            log.warn("Event received in invalid order; got " + event.getSeq() + ", expected " + c + "\nData: " + event.getData());
                        if (event.test(queue::add))
                            queue.notify();
                        else log.error("Failed to queue new input " + event);
                    }
                }

                @Override
                @SneakyThrows
                public int getAsInt() {
                    if (endl) return $endl();
                    if (pad) return $pad();

                    while (buf == null) {
                        synchronized (queue) {
                            while (queue.isEmpty())
                                queue.wait();
                        }
                        setBuf(queue.poll());
                    }

                    int i = this.i.incrementAndGet();
                    if (i == buf.length()) {
                        setBuf(null);
                        if (endlMode == EndlMode.OnDelegate)
                            return declareEndl();
                    }
                    if (buf == null) {
                        log.error("buf was unexpectedly null");
                        return -1;
                    }

                    var c = buf.charAt(i);
                    switch(c){
                        case '\r':
                            // ignore CR
                            return getAsInt();
                        case '\n':
                            if (endlMode == EndlMode.OnNewLine)
                                return declareEndl();
                    }
                    return c;
                }

                private void setBuf(String txt) {
                    //noinspection StringEquality
                    if(buf==txt)
                        return;
                    buf = txt;
                    i.set(0);
                }

                private int declareEndl() {
                    setBuf(null);
                    endl = true;
                    return '\n';
                }

                private int $endl() {
                    endl = false;
                    pad = true;
                    return -1;
                }

                private int $pad() {
                    pad = false;
                    return 0;
                }

                @Override
                public void closeSelf() {
                    queue.clear();
                }
            }
            this.name = "Adapter InputStream @ " + caller();
            var handler = new EventBusHandler();

            handler.addChildren(source);
            this.delegate = handler;

            source//.setExecutor(Executors.newSingleThreadExecutor())
                    .listen(handler);
            this.read = handler;
        }

        @Override
        public int read() {
            try {
                return read.getAsInt();
            } catch (Throwable t) {
                log.error("Could not read from " + this, t);
                return -1;
            }
        }

        @Override
        public int getCapabilities() {
            return Capability.Input.getAsInt();
        }

        @Override
        public Rewrapper<InputStream> input() {
            return Rewrapper.of(this);
        }

        @Override
        @SneakyThrows
        public void closeSelf() {
            if (delegate != null)
                delegate.close();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    @Slf4j
    @Value
    @EqualsAndHashCode(callSuper = true)
    class Output extends OutputStream implements DelegateStream {
        @lombok.experimental.Delegate(excludes = SelfCloseable.class)
        Container.Delegate<Output> container = new Delegate<>(this);
        @NonFinal @NonNull @Setter String name;
        ThrowingIntConsumer<IOException> write;
        ThrowingRunnable<IOException> flush;
        @Nullable AutoCloseable delegate;

        public Output(final OutputStream delegate) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.delegate = delegate;
            this.name = "Proxy OutputStream @ " + caller();
        }

        public Output(final Writer delegate) {
            this.write = delegate::write;
            this.flush = delegate::flush;
            this.delegate = delegate;
            this.name = "Writer delegating OutputStream @ " + caller();
        }

        public Output(final Logger log, final Level level) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.updateAndGet(buf -> {
                log.atLevel(level).log(buf.toString());
                return new StringWriter();
            });
            this.delegate = null;
            this.name = "Log delegating OutputStream @ " + caller();
        }

        public Output(final Consumer<String> handler) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.getAndUpdate(buf -> {
                handler.accept(buf.toString());
                return new StringWriter();
            });
            this.delegate = null;
            this.name = "Adapter OutputStream @ " + caller();
        }

        @Override
        public void write(int b) {
            try {
                write.accept(b);
            } catch (Throwable t) {
                log.error("Could not read from " + this, t);
            }
        }

        @Override
        public void flush() {
            try {
                flush.run();
            } catch (Throwable t) {
                log.error("Could not read from " + this, t);
            }
        }

        @Override
        public int getCapabilities() {
            return Capability.Output.getAsInt();
        }

        @Override
        public Rewrapper<OutputStream> output() {
            return Rewrapper.of(this);
        }

        @Override
        @SneakyThrows
        public void closeSelf() {
            if (delegate != null)
                delegate.close();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    @Slf4j
    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class IO implements DelegateStream, N.Consumer.$3<@Nullable Consumer<InputStream>, @Nullable Consumer<OutputStream>, @Nullable Consumer<OutputStream>> {
        public static final IO NULL = new IO(new ByteArrayInputStream(new byte[0]),StreamUtil.voidOutputStream(),StreamUtil.voidOutputStream());
        public static final IO SYSTEM = new IO(System.in, System.out, System.err);
        public static IO slf4j(Logger log) {return new IO(null,new Output(log::info), new Output(log::error));}

        @lombok.experimental.Delegate(excludes = SelfCloseable.class)
        Container.Delegate<IO> container = new Delegate<>(this);
        int initialCapabilities;
        Deque<IO> redirects;
        @NonFinal @NonNull @Setter String name;
        @NonFinal @Nullable IO parent;
        @Nullable InputStream in;
        @Nullable OutputStream out;
        @Nullable OutputStream err;
        @Getter AutoCloseable delegate;

        public IO name(String name) {this.name = name;return this;}
        public boolean isRedirect() {return in instanceof RedirectInput || out instanceof RedirectOutput || err instanceof RedirectOutput;}
        public boolean isNull() {return NULL.equals(this);}
        public boolean isSystem() {return SYSTEM.equals(this);}

        public IO and() {
            var it = this;
            while (!it.isRedirect() && it.parent != null)
                it = it.parent;
            if (it.isRedirect())
                return it;
            var io = new IO();
            io.redirect(this);
            return io;
        }
        public IO dev_null() {return redirect(NULL);}
        public IO system() {return redirect(SYSTEM);}
        public IO log(Logger log) {return redirect(slf4j(log));}
        public IO redirectErr(@Nullable OutputStream err) { return redirect(new IO(null,null,err));}
        public IO redirect(@Nullable InputStream in) { return redirect(new IO(in,null,null));}
        public IO redirect(@Nullable OutputStream out) { return redirect(new IO(null,out,null));}
        public IO redirect(@Nullable InputStream in, @Nullable OutputStream out) { return redirect(new IO(in, out, null));}
        public IO redirect(@Nullable OutputStream out, @Nullable OutputStream err) { return redirect(new IO(null,out,err));}
        public IO redirect(@Nullable InputStream in, @Nullable OutputStream out, @Nullable OutputStream err) { return redirect(new IO(in,out,err));}
        public IO redirect(@NotNull IO redirect) {
            if (!isRedirect()) {
                log.warn(String.format("Cannot attach redirect to IO container %s; returning IO.NULL", this));
                return NULL;
            }
            if (redirect.parent != null && !redirect.isSystem())
                redirect.detach();
            redirects.push(redirect);
            redirect.parent = this;
            return redirect;
        }
        public void detach() {
            if (parent == null || parent.redirects.remove(this))
                log.warn("Could not remove redirect from parent");
            else parent = null;
        }

        public IO() {
            this(Capability.values());
        }

        public IO(Capability... capabilities) {
            // default constructor
            this(null, null, null, capabilities);
        }

        public IO(
                @Nullable InputStream in,
                @Nullable OutputStream out,
                @Nullable OutputStream err,
                Capability... capabilities
        ) {
            this("IO @ " + caller(), null, in, out, err, Set.of(capabilities), Set.of());
        }

        @lombok.Builder(builderClassName = "Builder")
        private IO(
                @NotNull String name,
                @Nullable IO parent,
                @Nullable InputStream in,
                @Nullable OutputStream out,
                @Nullable OutputStream err,
                @Singular Set<Capability> initialCapabilities,
                @Singular Set<IO> redirects
        ) { // builder constructor
            this.name = name;
            this.parent = parent;
            this.initialCapabilities = Bitmask.combine(initialCapabilities.toArray(Capability[]::new));
            this.redirects = new ArrayDeque<>(redirects);
            this.in = obtainStream(in,Capability.Input, RedirectInput::new);
            this.out = obtainStream(out,Capability.Output,()->new RedirectOutput(Capability.Output));
            this.err = obtainStream(err,Capability.Error,()->new RedirectOutput(Capability.Error));
            this.delegate = ()->{
                if (this.in!=null)this.in.close();
                if (this.out!=null)this.out.close();
                if (this.err!=null)this.err.close();
            };
        }

        @Override
        public int getCapabilities() {
            return Bitmask.arrange(in != null, out != null, err != null);
        }

        public boolean hasCapability(Capability capability) {
            return Bitmask.isFlagSet(getCapabilities(), capability);
        }

        @Override public Rewrapper<InputStream> input() {return Rewrapper.ofSupplier(()->in);}
        @Override public Rewrapper<OutputStream> output() {return Rewrapper.ofSupplier(()->out);}
        @Override public Rewrapper<OutputStream> error() {return Rewrapper.ofSupplier(()->err);}

        public String getAlternateName() {return toInfoString(1, false);}

        private String toInfoString(int indent, boolean slim) {
            var sb = new StringBuilder(getName());

            final String here = '\n'+"|\t".repeat(indent-1)+"==> ";
            final String tabs = '\n'+"|\t".repeat(indent-1)+" -> ";
            final String desc = '\n'+"|\t".repeat(indent-1)+"  - ";

            if (parent != null)
                sb.append(here).append("Parent:").append(tabs).append(parent.toInfoString(indent+1,true));

            if (!slim && redirects.size() != 0) {
                sb.append(here).append("Redirects:");
                for (var redirect : redirects) {
                    sb.append(tabs);
                    if (redirect != null)
                        sb.append(redirect.toInfoString(indent+1, slim));
                    else sb.append("null");
                }
            }

            for (var capability : Capability.values()) {
                var list = stream(capability).collect(Collectors.toList());
                if (list.isEmpty()) {
                    continue;
                }
                sb.append(here).append(capability.getName()).append(":");
                stream(capability).forEach(tgt -> {
                    sb.append(tabs).append(tgt);
                    Optional.of(tgt)
                            .filter(DelegateStream.class::isInstance)
                            .map(DelegateStream.class::cast)
                            .map(DelegateStream::getDelegate)
                            .filter(Predicate.not(tgt::equals))
                            .ifPresent(delegate -> sb.append(desc).append("Delegate: ")
                                    .append(Named.$(delegate)));
                });
            }

            return sb.toString();
        }
        private <R> @Nullable R obtainStream(R basis, final Capability capability, final Supplier<R> fallback) {
            return Optional.ofNullable(basis)
                    .or(() -> Bitmask.isFlagSet(this.initialCapabilities, capability)
                            ? Optional.ofNullable(fallback.get())
                            : Optional.empty())
                    .orElse(null);
        }

        @Override
        @SneakyThrows
        public void closeSelf() {
            detach();
            for (var redirect : redirects)
                redirect.close();
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
        private <T extends Closeable> Stream<T> stream(final Capability capability) {
            return redirects.stream() // Stream.concat(redirect.stream(), Stream.of(SYSTEM))
                    .filter(it->Bitmask.isFlagSet(it.getCapabilities(), capability))
                    .map(it->{switch(capability){
                        case Input: return it.input();
                        case Output: return it.output();
                        case Error: return it.error();}
                        return null;})
                    .flatMap(Rewrapper::stream)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(Polyfill::uncheckedCast);
        }

        // todo: accept(Object) with autoconfiguration for any object

        public void accept(
                @Nullable Consumer<OutputStream> out,
                @Nullable Consumer<OutputStream> err
        ) {
            accept(null, out, err);
        }

        @Override
        public void accept(
                @Nullable Consumer<InputStream> in,
                @Nullable Consumer<OutputStream> out,
                @Nullable Consumer<OutputStream> err
        ) {
            Rewrapper.of( in).ifBothPresent(input(), java.util.function.Consumer::accept);
            Rewrapper.of(out).ifBothPresent(output(), java.util.function.Consumer::accept);
            Rewrapper.of(err).ifBothPresent(error(), java.util.function.Consumer::accept);
        }

        @Value
        @EqualsAndHashCode(callSuper = true)
        private class RedirectInput extends InputStream {
            Object monitor = new Object();

            @Override
            @Synchronized("monitor")
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

        @Value
        @EqualsAndHashCode(callSuper = true)
        private class RedirectOutput extends OutputStream {
            Object monitor = new Object();
            Capability capability;

            @Override
            @Synchronized("monitor")
            public void write(final int b) {
                try {
                    runOnOutput(capability, s->s.write(b));
                } catch (Throwable t) {
                    log.error("Error writing to Output", t);
                }
            }

            @Override
            @Synchronized("monitor")
            public void flush() {
                try {
                    runOnOutput(capability, OutputStream::flush);
                } catch (Throwable t) {
                    log.error("Error flushing Output", t);
                }
            }

            @Override
            public String toString() {
                return "RedirectOutput of " + getName();
            }
        }
    }
}
