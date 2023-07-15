package org.comroid.api;

import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.comroid.annotations.Convert;
import org.comroid.api.info.Log;
import org.comroid.util.Bitmask;
import org.comroid.util.StackTraceUtils;
import org.comroid.util.StreamUtil;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.api.Rewrapper.*;
import static org.comroid.util.StackTraceUtils.lessSimpleName;

public interface DelegateStream extends Container, Closeable, Named, Convertible {
    AutoCloseable getDelegate();

    @MagicConstant(flagsFromClass = Capability.class)
    default int getCapabilities() {
        return 0;
    }

    default Rewrapper<InputStream> input() {return empty();}
    default Rewrapper<OutputStream> output() {return Rewrapper.<OutputStream>empty().or(error());}
    default Rewrapper<OutputStream> error() {return Rewrapper.<OutputStream>empty().or(output());}

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
                    log.log(Level.SEVERE, "Could not read from " + delegate, t);
                    return -1;
                }
            }

            @Override
            public void close() {
                try {
                    delegate.close();
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Could not close " + delegate, t);
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
                    log.log(Level.SEVERE, "Could not write to " + delegate, t);
                }
            }

            @Override
            public void flush() {
                try {
                    delegate.flush();
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Could not flush " + delegate, t);
                }
            }

            @Override
            public void close() {
                try {
                    delegate.close();
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Could not close " + delegate, t);
                }
            }
        }
        return new Output(new WrapOutputStream(stream, logger));
    }

    @Convert
    default PrintStream toPrintStream() {
        return output().ifPresentMap(PrintStream::new);
    }

    private static UnsupportedOperationException unsupported(DelegateStream stream, Capability capability) {
        return new UnsupportedOperationException(String.format("%s has no support for %s", stream, capability));
    }

    private static String caller() {return StackTraceUtils.caller(1);}

    enum Capability implements BitmaskAttribute<Capability> {Input, Output, Error}
    enum EndlMode implements IntegerAttribute {Manual, OnNewLine, OnDelegate}

    @lombok.extern.java.Log
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Input extends InputStream implements DelegateStream, Provider<String> {
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
            this.name = "Reader delegating InputStream @ " + caller();
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
                //private boolean pad = true;

                @Override
                public void accept(Event<String> event) {
                    synchronized (queue) {
                        int c = this.c.incrementAndGet();
                        if (c != event.getSeq())
                            log.log(Level.WARNING, "Event received in invalid order; got " + event.getSeq() + ", expected " + c + "\nData: " + event.getData());
                        if (event.test(queue::add))
                            queue.notify();
                        else log.log(Level.SEVERE, "Failed to queue new input " + event);
                    }
                }

                @Override
                @SneakyThrows
                public int getAsInt() {
                    if (endl) return $endl();
                    //if (pad) return $pad();

                    while (buf == null) {
                        synchronized (queue) {
                            while (queue.isEmpty())
                                queue.wait();
                        }
                        setBuf(queue.poll());
                    }

                    int i = this.i.incrementAndGet();
                    if (i == buf.length()) {
                        if (endlMode == EndlMode.OnDelegate)
                            return declareEndl();
                        else setBuf(null);
                    }
                    if (buf == null) {
                        log.log(Level.SEVERE, "buf was unexpectedly null");
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
                    //pad = true;
                    return -1;
                }

                private int $pad() {
                    //pad = false;
                    return ' ';
                }

                @Override
                public void closeSelf() {
                    queue.clear();
                }
            }
            this.name = lessSimpleName(source.getClass()) + " InputStream @ " + caller();
            var handler = new EventBusHandler();

            handler.addChildren(source);
            this.delegate = handler;

            source.listen(IO.EventKey_Input, handler);
            this.read = handler;
        }

        @Override
        public int read() {
            try {
                return read.getAsInt();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Could not read from " + this, t);
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
        public CompletableFuture<String> get() {
            return CompletableFuture.supplyAsync(()->{
                var buf = new StringWriter();
                int i;
                while ((i = read())!=-1)
                    buf.write(i);
                return buf.toString();
            });
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

    @lombok.extern.java.Log
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
                log.log(level, buf.toString());
                return new StringWriter();
            });
            this.delegate = null;
            this.name = "Log delegating OutputStream @ " + caller();
        }

        public Output(final Consumer<byte[]> handler, final int bufferSize) {
            final var writer = new AtomicReference<>(new ByteArrayOutputStream(bufferSize));
            this.write = b -> writer.get().write(b);
            this.flush = () -> writer.getAndUpdate(buf -> {
                handler.accept(buf.toByteArray());
                return new ByteArrayOutputStream(bufferSize);
            });
            this.delegate = handler instanceof AutoCloseable ? (AutoCloseable) handler : null;
            this.name = lessSimpleName(handler.getClass()) + " OutputStream @ " + caller();
        }

        public Output(final Consumer<String> handler) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.getAndUpdate(buf -> {
                handler.accept(buf.toString());
                return new StringWriter();
            });
            this.delegate = handler instanceof AutoCloseable ? (AutoCloseable) handler : null;
            this.name = lessSimpleName(handler.getClass()) + " OutputStream @ " + caller();
        }

        public Output(final Event.Bus<String> bus, Capability capability) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.getAndUpdate(buf -> {
                bus.publish(IO.eventKey(capability), buf.toString());
                return new StringWriter();
            });
            this.delegate = bus;
            this.name = lessSimpleName(bus.getClass()) + " OutputStream @ " + caller();
        }

        private Output(@NotNull final PipelineAdapter adapter) {
            final var writer = new AtomicReference<>(new StringWriter());
            this.write = c -> writer.get().write(c);
            this.flush = () -> writer.getAndUpdate(buf -> {
                adapter.apply(buf.toString())
                        .filter(Objects::nonNull)
                        .forEachOrdered(adapter::apply);
                return new StringWriter();
            });
            this.delegate = adapter;
            this.name = "Pipeline OutputStream @ " + caller();
        }

        private Output(@NotNull final SegmentAdapter adapter, final int length) {
            final var buffer = new AtomicReference<>(new byte[length]);
            final var cursor = new AtomicInteger(0);
            this.write = c -> {
                var b = (byte) c;
                buffer.updateAndGet(data -> {
                    if (cursor.get() < data.length)
                        data[cursor.getAndIncrement()] = b;
                    else {
                        data = adapter.apply(data);
                        if (data != null)
                            adapter.accept(data);
                        cursor.set(0);
                    }
                    return Optional.ofNullable(data).orElseGet(() -> new byte[length]);
                });
            };
            this.flush = ()->{};
            this.delegate = adapter;
            this.name = "Segmented OutputStream @ " + caller();
        }

        //region Stream OPs
        @ApiStatus.Experimental
        public Output peek(final Consumer<@NotNull String> action) {
            return filter(txt -> {
                action.accept(txt);
                return true;
            });
        }

        @ApiStatus.Experimental
        public Output filter(final Predicate<@NotNull String> action) {
            return map(str -> action.test(str) ? str : null);
        }

        @ApiStatus.Experimental
        public Output map(final Function<@NotNull String, @Nullable String> action) {
            return flatMap(str -> Stream.of(action.apply(str)));
        }

        @ApiStatus.Experimental
        public Output flatMap(final Function<@NotNull String, Stream<@Nullable String>> action) {
            Output out = this;
            if (!(out.delegate instanceof PipelineAdapter))
                out = new Output(new PipelineAdapter(this, action.andThen(x->x
                        .filter(Objects::nonNull)
                //        .map(y->y+'\n')
                )));
            else ((PipelineAdapter) out.delegate).actions.add(action);
            return out;
        }
        //endregion

        //region Segment OPs
        @ApiStatus.Experimental
        public Output peekSegment(final Consumer<byte @NotNull []> action) throws IllegalStateException {
            return filterSegment(data -> {
                action.accept(data);
                return true;
            });
        }

        @ApiStatus.Experimental
        public Output filterSegment(final Predicate<byte @NotNull []> action) throws IllegalStateException {
            return mapSegment(data -> action.test(data) ? data : null);
        }

        @ApiStatus.Experimental
        public Output mapSegment(final Function<byte @NotNull [], byte @Nullable []> action) throws IllegalStateException {
            Output out = this;
            if (out.delegate instanceof SegmentAdapter)
                ((SegmentAdapter) out.delegate).actions.add(action);
            else throw new IllegalStateException("Segment OPs are only allowed after segment(int) has been called");
            return out;
        }

        @ApiStatus.Experimental
        public Output segment(int length) {
            return new Output(new SegmentAdapter(this), length);
        }
        //endregion

        //region Packet Converter
        @ApiStatus.Experimental
        public <H,B> Packet<H,B> packet(
                int headLength,
                Function<byte@NotNull[], H> headFactory,
                ToIntFunction<H> bodyLength,
                Function<byte@NotNull[], B> bodyFactory
        ) {
            return new Packet<>(headLength, headFactory, bodyLength, bodyFactory);
        }
        //endregion

        @Override
        public void write(int b) {
            try {
                write.accept(b);
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Could not read from " + this, t);
            }
        }

        @Override
        public void flush() {
            try {
                flush.run();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Could not read from " + this, t);
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

        @SneakyThrows
        public void accept(String txt) {
            accept(txt.getBytes());
        }

        @SneakyThrows
        public void accept(byte[] data) {
            write(data);
            flush();
        }

        @Value
        @EqualsAndHashCode(callSuper = true)
        private static class PipelineAdapter extends PrintStream {
            List<Function<String, Stream<String>>> actions;

            private PipelineAdapter(OutputStream output, Function<String, Stream<String>> action) {
                super(output, true);
                this.actions = new ArrayList<>(Collections.singletonList(action));
            }

            public Stream<@Nullable String> apply(@NotNull String txt) {
                var stream = Stream.of(txt);
                for (var action : actions)
                    stream = stream.flatMap(action).filter(Objects::nonNull);
                return stream;
            }
        }

        @Value
        @EqualsAndHashCode(callSuper = true)
        private static class SegmentAdapter extends DelegateStream.Output {
            List<Function<byte@NotNull[], byte@Nullable[]>> actions = new ArrayList<>();

            private SegmentAdapter(OutputStream output) {
                super(output);
            }

            public byte@Nullable[] apply(byte@NotNull[] data) {
                for (var action : actions)
                    if (data != null)
                        data = action.apply(data);
                    else return null;
                return data;
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    class Packet<H,B> extends ByteArrayOutputStream {
        @lombok.experimental.Delegate(types = Event.IBus.class)
        Event.Bus<Packet<H,B>.Pair> bus = new Event.Bus<>();
        int headLength;
        Function<byte @NotNull [], H> headFactory;
        ToIntFunction<H> bodyLength;
        Function<byte @NotNull [], B> bodyFactory;
        AtomicReference<@Nullable H> head = new AtomicReference<>();

        public Packet(int headLength,
                      Function<byte @NotNull [], H> headFactory,
                      ToIntFunction<H> bodyLength,
                      Function<byte @NotNull [], B> bodyFactory) {
            this.headLength = headLength;
            this.headFactory = headFactory;
            this.bodyLength = bodyLength;
            this.bodyFactory = bodyFactory;

            buf = new byte[headLength];
        }

        @Override
        @SneakyThrows
        public synchronized void write(int b) {
            super.write(b);
            if (count == headLength)
                flush();
        }

        @Override
        public void flush() {
            H head = this.head.get();
            if (head != null) {
                var body = bodyFactory.apply(buf);
                bus.publish(new Pair(head, body));
                clear(headLength);
            } else {
                head = headFactory.apply(buf);
                var bodyLen = bodyLength.applyAsInt(head);
                this.head.set(head);
                clear(bodyLen);
            }
        }

        public void clear(int len) {
            buf = new byte[len];
            count = 0;
            if (len == headLength)
                this.head.set(null);
        }

        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public class Pair {
            H head;
            B body;
        }
    }

    @lombok.extern.java.Log
    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class IO implements DelegateStream, N.Consumer.$3<@Nullable Consumer<InputStream>, @Nullable Consumer<OutputStream>, @Nullable Consumer<OutputStream>> {
        public static final String EventKey_Input = "stdin";
        public static final String EventKey_Output = "stdout";
        public static final String EventKey_Error = "stderr";
        public static final IO NULL = new IO(new ByteArrayInputStream(new byte[0]),StreamUtil.voidOutputStream(),StreamUtil.voidOutputStream());
        public static final IO SYSTEM = new IO(System.in, System.out, System.err);
        public static IO slf4j(Logger log) {return new IO(null,new Output(log::info), new Output(log::severe));}

        @lombok.experimental.Delegate(excludes = SelfCloseable.class)
        Container.Delegate<IO> container = new Delegate<>(this);
        int initialCapabilities;
        Deque<IO> redirects;
        @NonFinal @NonNull @Setter String name;
        @NonFinal @Nullable IO parent;
        @NonFinal @Nullable InputStream input;
        @NonFinal @Nullable OutputStream output;
        @NonFinal @Nullable OutputStream error;
        @NonFinal @Setter OutputMode inputMode = OutputMode.FirstOnly;
        @NonFinal @Setter OutputMode outputMode = OutputMode.All;
        @NonFinal @Setter OutputMode errorMode = OutputMode.All;
        @Getter AutoCloseable delegate;

        public IO name(String name) {this.name = name;return this;}
        public boolean isRedirect() {return initialCapabilities != 0;}
        public boolean isNull() {return NULL.equals(this);}
        public boolean isSystem() {return SYSTEM.equals(this);}

        public IO redirectToNull() {return redirect(NULL);}
        public IO redirectToLogger(Logger log) {return redirect(slf4j(log));}
        public IO redirectToSystem() {return redirect(SYSTEM);}
        public IO redirectToEventBus(Event.Bus<String> bus) {
            return redirect(
                    new Input(bus),
                    new Output(bus, Capability.Output),
                    new Output(bus, Capability.Error));
        }
        @SneakyThrows
        public IO redirectToSocket(Socket socket) {
            return redirect(socket.getInputStream(), socket.getOutputStream());
        }
        public IO redirectErr(@Nullable OutputStream err) { return redirect(new IO(null,null,err));}
        public IO redirect(@Nullable InputStream in) { return redirect(new IO(in,null,null));}
        public IO redirect(@Nullable OutputStream out) { return redirect(new IO(null,out,null));}
        public IO redirect(@Nullable InputStream in, @Nullable OutputStream out) { return redirect(new IO(in, out, null));}
        public IO redirect(@Nullable OutputStream out, @Nullable OutputStream err) { return redirect(new IO(null,out,err));}
        public IO redirect(@Nullable InputStream in, @Nullable OutputStream out, @Nullable OutputStream err) { return redirect(new IO(in,out,err));}
        public IO redirect(@NotNull IO redirect) {
            if (!isRedirect()) {
                log.log(Level.WARNING, String.format("Cannot attach redirect to %s; returning /dev/null", this));
                return NULL;
            }
            if (redirect.parent != null && !redirect.isSystem())
                redirect.detach();
            redirects.push(redirect);
            redirect.parent = this;
            return this;
        }
        public IO rewireInput(@Nullable Function<@NotNull Input, ? extends InputStream> in) {
            return rewire(in,null);
        }
        public IO rewireOutput(@Nullable Function<@NotNull Output, ? extends OutputStream> out) {
            return rewire(null,out);
        }
        public IO rewireError(@Nullable Function<@NotNull Output, ? extends OutputStream> err) {
            return rewire(null,null,err);
        }
        public IO rewireOE(@Nullable Function<@NotNull Output, ? extends OutputStream> out) {
            return rewire(null,out,out);
        }
        public IO rewire(@Nullable Function<@NotNull Input,  ? extends InputStream>   in,
                         @Nullable Function<@NotNull Output, ? extends OutputStream> out) {
            return rewire(in,out,null);
        }
        public IO rewire(@Nullable Function<@NotNull Input,  ? extends InputStream>   in,
                         @Nullable Function<@NotNull Output, ? extends OutputStream> out,
                         @Nullable Function<@NotNull Output, ? extends OutputStream> err) {
            this.name = "Rewired " + name;
            this.input = mapStream(Input.class,  RedirectInput.class,  input(),  Input::new,  in,         RedirectInput::new,                          RedirectInput::new);
            this.output =mapStream(Output.class, RedirectOutput.class, output(), Output::new, out, s->new RedirectOutput(s,Capability.Output), ()->new RedirectOutput(Capability.Output));
            this.error =mapStream(Output.class, RedirectOutput.class, error(),  Output::new, err, s->new RedirectOutput(s,Capability.Error),  ()->new RedirectOutput(Capability.Error));
            return this;
        }
        private static <Base, Adapter extends DelegateStream, Result extends DelegateStream> @Nullable Result mapStream(
                final Class<Adapter> typeA,
                final Class<Result> typeR,
                final @NotNull Rewrapper<@Nullable Base> supp,
                final @NotNull Function<@NotNull Base, ? extends Adapter> prep,
                final @Nullable Function<@NotNull Adapter, ? extends Base> func,
                final @NotNull Function<@NotNull Base, @NotNull Result> wrap,
                final @NotNull Rewrapper<Result> def
        ) {
            // ((supp -> cast+prep -> func) / supp) -> cast/wrap/def
            return Optional.ofNullable(func)
                    // supp -> cast+prep -> func
                    .flatMap(fx->supp.wrap()
                            // cast+prep
                            .flatMap(x -> Optional.of(x)
                                    // cast
                                    .filter(typeA::isInstance)
                                    .map(typeA::cast)
                                    // prep
                                    .or(()->Optional.of(x).map(prep)))
                            // func
                            .map(fx))
                    .map(Polyfill::<Base>uncheckedCast)
                    // supp
                    .or(supp::wrap)
                    // cast+wrap
                    .flatMap(x->Optional.of(x)
                            // cast
                            .filter(typeR::isInstance)
                            .map(typeR::cast)
                            // wrap
                            .or(()->Optional.of(x).map(wrap)))
                    // default if necessary
                    .or(()->def.wrap().filter($->func!=null))
                    // adjust name
                    .map(x->x.setName("Rewired " + x.getName()))
                    .map(Polyfill::<Result>uncheckedCast)
                    .orElse(null);
        }
        public void detach() {
            if (parent == null || parent.redirects.remove(this))
                log.log(Level.WARNING, "Could not remove redirect from parent");
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
                @Nullable InputStream input,
                @Nullable OutputStream output,
                @Nullable OutputStream error,
                Capability... capabilities
        ) {
            this((capabilities.length == 0 ? "Container" : "Redirect") + " IO @ " + caller(), null, input, output, error, Set.of(capabilities), Set.of());
        }

        @lombok.Builder(builderClassName = "Builder")
        private IO(
                @NotNull String name,
                @Nullable IO parent,
                @Nullable InputStream input,
                @Nullable OutputStream output,
                @Nullable OutputStream error,
                @Singular Set<Capability> initialCapabilities,
                @Singular Set<IO> redirects
        ) { // builder constructor
            this.name = name;
            this.parent = parent;
            this.initialCapabilities = Bitmask.combine(initialCapabilities.toArray(Capability[]::new));
            this.redirects = new ArrayDeque<>(redirects);
            this.input = obtainStream(input,Capability.Input, RedirectInput::new);
            this.output = obtainStream(output,Capability.Output,()->new RedirectOutput(Capability.Output));
            this.error = obtainStream(error,Capability.Error,()->new RedirectOutput(Capability.Error));
            this.delegate = ()->{
                if (this.input !=null)this.input.close();
                if (this.output !=null)this.output.close();
                if (this.error !=null)this.error.close();
            };
        }

        @Override
        public int getCapabilities() {
            return Bitmask.arrange(input != null, output != null, error != null);
        }

        public boolean hasCapability(Capability capability) {
            return Bitmask.isFlagSet(getCapabilities(), capability);
        }

        @Override public Rewrapper<InputStream> input() {return ofSupplier(()-> input);}
        @Override public Rewrapper<OutputStream> output() {return ofSupplier(()-> output);}
        @Override public Rewrapper<OutputStream> error() {return ofSupplier(()-> error);}

        public String getAlternateName() {return toInfoString(1);}

        private String toInfoString(int indent) {
            var sb = new StringBuilder(getName());

            final String here = '\n'+"|\t".repeat(indent-1)+"==> ";
            final String tabs = '\n'+"|\t".repeat(indent-1)+" -> ";
            final String desc = '\n'+"|\t".repeat(indent-1)+"  - ";

            if (parent != null)
                sb.append(here).append("Parent:").append(tabs).append(parent.getName());

            if (redirects.size() != 0) {
                sb.append(here).append("Redirects:");
                for (var redirect : redirects) {
                    sb.append(tabs);
                    if (redirect != null)
                        sb.append(redirect.toInfoString(indent+1));
                    else sb.append("null");
                }
            }

            for (var capability : Capability.values()) {
                var list = stream(capability).collect(Collectors.toList());
                if (list.isEmpty()) {
                    continue;
                }
                sb.append(here).append(capability.getName()).append(" (").append(mode(capability).getName()).append("):");
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

        // todo: accept(Object) with autoconfiguration for any object

        public void accept(
                @Nullable Consumer<InputStream> in,
                @Nullable Consumer<OutputStream> out
        ) {
            accept(in, out, null);
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

        @Override
        @SneakyThrows
        public void closeSelf() {
            detach();
            for (var redirect : redirects)
                redirect.close();
            delegate.close();
        }

        private int runOnInput(ThrowingToIntFunction<InputStream, ? extends Throwable> action) throws Throwable {
            if (inputMode == OutputMode.All)
                throw new UnsupportedOperationException("Input mode "+inputMode.name()+" is not supported");
            var array = this.<InputStream>stream(Capability.Input).toArray(InputStream[]::new);
            if (array.length == 0)
                throw unsupported(this, Capability.Input);
            InputStream use;
            switch (inputMode) {
                case FirstOnly:
                    use = array[0];
                    break;
                case LastOnly:
                    use = array[array.length - 1];
                    break;
                default:
                    throw new RuntimeException("invalid input mode " + inputMode);
            }
            return action.applyAsInt(use);
        }
        private void runOnOutput(Capability capability, ThrowingConsumer<OutputStream, ? extends Throwable> action) throws Throwable {
            var array = this.<OutputStream>stream(capability).toArray(OutputStream[]::new);
            switch (outputMode) {
                case FirstOnly:
                    action.accept(array[0]);
                    break;
                case LastOnly:
                    action.accept(array[array.length - 1]);
                    break;
                case All:
                    for (var use : array)
                        action.accept(use);
                    break;
            }
        }
        public <T extends Closeable> Stream<T> stream(final Capability capability) {
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
        public OutputMode mode(Capability capability) {
            switch (capability) {
                case Input: return inputMode;
                case Output: return outputMode;
                case Error: return errorMode;
            }
            throw new RuntimeException("unreachable");
        }
        public static String eventKey(Capability capability) {
            switch (capability) {
                case Input: return EventKey_Input;
                case Output: return EventKey_Output;
                case Error: return EventKey_Error;
            }
            throw new RuntimeException("unreachable");
        }

        public enum OutputMode implements IntegerAttribute { FirstOnly, LastOnly, All }

        @Value
        @EqualsAndHashCode(callSuper = true)
        private class RedirectInput extends Input {

            private RedirectInput() {
                this(new InputStream() {
                    private final Object monitor = new Object();
                    @Override
                    @Synchronized("monitor")
                    public int read() {
                        try {
                            return runOnInput(InputStream::read);
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, "Error reading from InputStream", t);
                            return -1;
                        }
                    }
                });
            }

            private RedirectInput(InputStream delegate) {
                super(delegate);
            }

            @NotNull
            @Override
            public String getName() {
                return "RedirectInput of " + super.getName();
            }
        }

        @Value
        @EqualsAndHashCode(callSuper = true)
        private class RedirectOutput extends Output {
            Capability capability;

            private RedirectOutput(Capability capability) {
                this(new OutputStream() {
                    private final Object monitor = new Object();

                    @Override
                    @Synchronized("monitor")
                    public void write(final int b) {
                        try {
                            runOnOutput(capability, s->s.write(b));
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, "Error writing to Output", t);
                        }
                    }

                    @Override
                    @Synchronized("monitor")
                    public void flush() {
                        try {
                            runOnOutput(capability, OutputStream::flush);
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, "Error flushing Output", t);
                        }
                    }
                }, capability);
            }

            private RedirectOutput(OutputStream delegate, Capability capability) {
                super(delegate);
                this.capability = capability;
            }

            @NotNull
            @Override
            public String getName() {
                return "RedirectOutput of " + super.getName();
            }
        }
    }
}
