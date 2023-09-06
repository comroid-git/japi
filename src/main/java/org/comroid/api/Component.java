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
import java.lang.annotation.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
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

    default BackgroundTask<Component> execute(ScheduledExecutorService scheduler, Duration tickRate) {
        return new BackgroundTask<>(this, Component::tick, tickRate.toMillis(), scheduler).activate(ForkJoinPool.commonPool());
    }

    default void start() {
        initialize();
    }

    default void stop() {
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
        private @Setter @Nullable Component parent;
        private @Setter @Nullable String name = StackTraceUtils.lessSimpleName(getClass()) + '#' + hashCode();

        public Base(Object... children) {
            this((Component)null, children);
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
                if (!test(this, State.PreInit))
                    return;
                var components = parent != null ? parent.getChildren().stream()
                        .flatMap(cast(Component.class))
                        .collect(Collectors.toMap(Object::getClass, Function.identity()))
                        : Map.<Class<?>, Component>of();
                Log.at(Level.FINE, "Initializing "+this);
                var futures = new ArrayList<CompletableFuture<?>>();
                for (var dependency : requires())
                    components.entrySet().stream()
                            .filter(e -> dependency.isAssignableFrom(e.getKey()))
                            .map(Map.Entry::getValue)
                            .filter(c -> c.getCurrentState() == State.PreInit)
                            .peek(c -> Log.at(Level.FINE, "Initializing dependency of " + this + " first: " + c))
                            .map(c -> CompletableFuture.supplyAsync(() -> {
                                c.initialize();
                                return null;
                            }))
                            .forEach(futures::add);
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                $initialize();
                runOnChildren(Initializable.class, Initializable::initialize,it->test(it,State.PreInit));
                pushState(State.LateInit);

                lateInitialize();
            } catch (Throwable t) {
                Log.at(Level.SEVERE, "Could not initialize " + getName() + "; " + t.getMessage(), t);
                terminate();
            }
        }

        @Override
        public final void lateInitialize() {
            if (!test(this, State.Init) && !pushState(State.LateInit))
                return;
            runOnChildren(LifeCycle.class, LifeCycle::lateInitialize,it->test(it,State.Init));

            $lateInitialize();
            pushState(State.Active);
        }

        @Override
        //@PostUpdate
        public final void tick() {
            $tick();
            runOnChildren(Tickable.class, Tickable::tick, it->test(it,State.Active));
        }

        @Override
        public void stop() {
            terminate();
        }

        @Override
        public final void earlyTerminate() {
            pushState(State.EarlyTerminate);
            runOnChildren(LifeCycle.class, LifeCycle::earlyTerminate,it->test(it,State.Active));
            $earlyTerminate();
        }

        @Override
        //@PreRemove @PreDestroy
        public final void terminate() {
            earlyTerminate();

            pushState(State.Terminate);
            $terminate();
            runOnChildren(LifeCycle.class, LifeCycle::terminate,it->test(it,State.EarlyTerminate));

            pushState(State.PostTerminate);
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
            return it instanceof Component && Bitmask.isFlagSet(((Component) it).getCurrentState().mask, state);
        }

        private boolean pushState(State state) {
            if (currentState == state)
                return false; // avoid pushing same state twice
            previousState = currentState;
            currentState = state;
            Log.at(Level.INFO, getName() + " changed into state: " + currentState);
            return true;
        }
    }
}
