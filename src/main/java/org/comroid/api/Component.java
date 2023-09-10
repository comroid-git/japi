package org.comroid.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.comroid.api.info.Log;
import org.comroid.util.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import java.io.Closeable;
import java.lang.annotation.*;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.util.Streams.*;

public interface Component extends Container, LifeCycle, Tickable, Named {

    @Override
    @PostLoad
    @PostConstruct
    void initialize();

    @Override
    @PostUpdate
    void tick();

    @Override
    @PreRemove
    @PreDestroy
    void terminate();

    State getCurrentState();

    default boolean isActive() {
        return getCurrentState() == State.Active;
    }

    @Nullable Component getParent();

    @Contract("_ -> this")
    Component setParent(@Nullable Component parent);

    default String getFullName() {
        return Optional.ofNullable(getParent())
                .map(Component::getFullName)
                .map(pName -> pName + '.' + getName())
                .orElseGet(this::getName);
    }

    default <T extends Component> Stream<T> components(@Nullable Class<T> type) {
        return streamChildren(type);
    }

    default <T extends Component> Rewrapper<T> component(@Nullable Class<T> type) {
        return () -> components(type).findAny().orElse(null);
    }

    default List<Class<? extends Component>> requires() {
        final var type = getClass();
        return Cache.get(type.getCanonicalName() + "@Requires", () -> Arrays
                .stream(type.getAnnotationsByType(Requires.class))
                .map(Requires::value)
                .flatMap(Arrays::stream)
                .toList());
    }

    default UncheckedCloseable execute(ScheduledExecutorService scheduler, Duration tickRate) {
        var task = scheduler.scheduleAtFixedRate(() -> {
            if (testState(State.PreInit))
                initialize();
            tick();
        },0,tickRate.toMillis(), TimeUnit.MILLISECONDS);
        final UncheckedCloseable closeable = ()-> {
            task.cancel(true);
            terminate();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(closeable::close));
        return closeable;
    }

    default void start() {
        initialize();
    }

    default void stop() {
        terminate();
    }

    default boolean testState(State state) {
        return Bitmask.isFlagSet(getCurrentState().mask, state);
    }

    @Override
    default void closeSelf() throws Exception {
        terminate();
    }

    enum State implements BitmaskAttribute<State> {
        PreInit,
        Init,
        LateInit(Init),

        Active,

        EarlyTerminate,
        Terminate(EarlyTerminate),
        PostTerminate(PreInit);

        private final int mask;

        State(State... ext) {
            this.mask = Bitmask.nextFlag(State.class) | Bitmask.combine(ext);
        }

        public int getAsInt() {
            return mask;
        }
    }

    /**
     * declare a module dependency
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Requires {
        Class<? extends Component>[] value();
    }

    @Getter
    class Base extends Container.Base implements Component {
        private State currentState = State.PreInit;
        private State previousState = State.PreInit;
        private @Setter
        @Nullable Component parent;
        private @Setter
        @Nullable String name = "%s#%x".formatted(StackTraceUtils.lessSimpleName(getClass()), hashCode());

        public Base(Object... children) {
            this((Component) null, children);
        }

        public Base(@Nullable Component parent, Object... children) {
            super(children);
            this.parent = parent;
        }

        public Base(@Nullable String name, Object... children) {
            this(null, name, children);
        }

        public Base(@Nullable Component parent, @Nullable String name, Object... children) {
            super(children);
            this.parent = parent;
            this.name = name;
        }

        @Override
        public Object addChildren(@Nullable Object... children) {
            for (Object child : children)
                if (child instanceof Component)
                    ((Component) child).setParent(this);
            return super.addChildren(children);
        }

        @Override
        public void start() {
            initialize();
        }

        @Override
        @SneakyThrows
        //@PreRemove @PreDestroy
        public final void initialize() {
            try {
                if (!testState(State.PreInit))
                    return;
                Log.at(Level.FINE, "Initializing " + this);
                runOnDependencies(Component::initialize).join();
                $initialize();
                runOnChildren(Initializable.class, Initializable::initialize, it -> test(it, State.PreInit));
                pushState(State.LateInit);

                lateInitialize();
            } catch (Throwable t) {
                Log.at(Level.SEVERE, "Could not initialize " + getName() + "; " + t.getMessage(), t);
                terminate();
            }
        }

        @Override
        public final void lateInitialize() {
            if (!testState(State.Init) && !pushState(State.LateInit))
                return;
            runOnDependencies(Component::lateInitialize).join();
            $lateInitialize();
            runOnChildren(LifeCycle.class, LifeCycle::lateInitialize, it -> test(it, State.Init));

            pushState(State.Active);
        }

        @Override
        //@PostUpdate
        public final void tick() {
            if (!testState(State.Active))
                return;
            try {
                Log.at(Level.FINER, MessageFormat.format("Ticking {0}", this));
                $tick();
                runOnChildren(Tickable.class, Tickable::tick, it -> test(it, State.Active));
            } catch (Throwable t) {
                Log.at(Level.WARNING, "Error in tick for %s".formatted(this), t);
            }
        }

        @Override
        public void stop() {
            terminate();
        }

        @Override
        public final void earlyTerminate() {
            pushState(State.EarlyTerminate);
            runOnChildren(LifeCycle.class, LifeCycle::earlyTerminate, it -> test(it, State.Active));
            $earlyTerminate();
        }

        @Override
        //@PreRemove @PreDestroy
        public final void terminate() {
            if (testState(State.PostTerminate))
                return;
            try {
                earlyTerminate();

                pushState(State.Terminate);
                $terminate();
                runOnChildren(LifeCycle.class, LifeCycle::terminate, it -> test(it, State.EarlyTerminate));

                pushState(State.PostTerminate);
            } catch (Throwable t) {
                Log.at(Level.WARNING, "Could not correctly terminate %s".formatted(this));
            }
        }

        protected void $initialize() {
        }

        protected void $lateInitialize() {
        }

        protected void $tick() {
        }

        protected void $earlyTerminate() {
        }

        protected void $terminate() {
        }

        private static <T> boolean test(T it, State state) {
            return it instanceof Component && ((Component) it).testState(state);
        }

        private boolean pushState(State state) {
            if (currentState == state)
                return false; // avoid pushing same state twice
            previousState = currentState;
            currentState = state;
            Log.at(Level.FINE, getName() + " changed into state: " + currentState);
            return true;
        }

        private CompletableFuture<Void> runOnDependencies(final ThrowingConsumer<Component, Throwable> action) {
            if (getParent() == null)
                return CompletableFuture.completedFuture(null);
            final var wrap = action.wrap();
            final var futures = requires().stream()
                    .flatMap(dependency -> getParent().getChildren().stream()
                            .filter(e -> dependency.isAssignableFrom(e.getClass()))
                            .flatMap(cast(Component.class)))
                    .filter(c -> c.testState(State.PreInit))
                    .peek(c -> Log.at(Level.FINE, "Initializing dependency of %s first: %s".formatted(this, c)))
                    .map(c -> CompletableFuture.supplyAsync(() -> {
                        wrap.accept(c);
                        return null;
                    }))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures);
        }
    }
}
