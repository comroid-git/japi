package org.comroid.api.tree;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.*;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataStructure;
import org.comroid.annotations.Annotations;
import org.comroid.annotations.Ignore;
import org.comroid.api.attr.BitmaskAttribute;
import org.comroid.api.attr.EnabledState;
import org.comroid.api.attr.Named;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.exc.ThrowingConsumer;
import org.comroid.api.info.Log;
import org.comroid.api.func.util.Bitmask;
import org.comroid.api.func.util.Cache;
import org.comroid.api.info.Constraint;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.api.func.util.Streams.Multi.*;
import static org.comroid.api.func.util.Streams.cast;

@Ignore
public interface Component extends Container, LifeCycle, Tickable, EnabledState, Named {
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

    default boolean isSubComponent() {
        return false;
    }

    default String getFullName() {
        return Optional.ofNullable(getParent())
                .map(Component::getFullName)
                .map(pName -> pName + '.' + getName())
                .orElseGet(this::getName);
    }

    default <T extends Component> Stream<T> components(@Nullable Class<? super T> type) {
        return Stream.concat(
                streamChildren(type),
                isSubComponent()
                        ? Stream.of(getParent())
                        .filter(Objects::nonNull)
                        .flatMap(comp -> comp.components(type))
                        : empty());
    }

    default <T extends Component> Wrap<T> component(@Nullable Class<? super T> type) {
        return () -> uncheckedCast(components(type).findAny().orElse(null));
    }

    default Set<Dependency<?>> dependencies() {
        return dependencies(getClass());
    }

    default UncheckedCloseable execute(ScheduledExecutorService scheduler, Duration tickRate) {
        var task = scheduler.scheduleAtFixedRate(() -> {
            if (testState(State.PreInit))
                initialize();
            tick();
        }, 0, tickRate.toMillis(), TimeUnit.MILLISECONDS);
        final UncheckedCloseable closeable = () -> {
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

    //static List<Class<? extends Component>> includes()

    static Set<Dependency<?>> dependencies(Class<? extends Component> type) {
        return Cache.get("dependencies of " + type.getCanonicalName(), () -> {
            var struct = DataStructure.of(type);
            return Stream.concat(
                            struct.getProperties().values().stream()
                                    //.flatMap(prop -> prop.annotations.stream())
                                    //.map(Polyfill::<Annotations.Result<Inject>>uncheckedCast)
                                    .filter(prop -> prop.annotations.stream()
                                            .anyMatch(result -> result.getAnnotation().annotationType().equals(Inject.class)))
                                    .map(prop -> {
                                        var inject = Polyfill.<Annotations.Result<Inject>>uncheckedCast(prop.getAnnotation(Inject.class));
                                        var anno = inject.getAnnotation();
                                        return new Dependency<>(
                                                Optional.of(anno.value()).filter(String::isEmpty).orElse(prop.name),
                                                uncheckedCast(prop.getType().getTargetClass()),
                                                anno.required(),
                                                uncheckedCast(prop));
                                    }),
                            Annotations.findAnnotations(Requires.class, type)
                                    .flatMap(requires -> Arrays.stream(requires.getAnnotation().value())
                                            .map(cls -> new Dependency<>("", cls, true, null))))
                    .collect(Collectors.toUnmodifiableSet());
        });
    }

    enum State implements BitmaskAttribute<State> {
        PreInit,
        Init,
        LateInit(Init),

        Active,

        EarlyTerminate,
        Terminate(EarlyTerminate),
        PostTerminate(PreInit);

        private final long mask;

        State(State... ext) {
            this.mask = Bitmask.nextFlag(State.class) | Bitmask.combine(ext);
        }

        public long getAsLong() {
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

    /**
     * declare field injection
     * occurs before initialization
     * if all are defaults, then name and type from field are used
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    @interface Inject {
        /**
         * @return filter by name match, unless length == 0
         */
        String value() default "";

        /**
         * @return filter by type match
         */
        Class<? extends Component> type() default Component.class;

        /**
         * @return whether to throw an exception if the dependency cannot be satisfied
         */
        boolean required() default true;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Dependency<T extends Component> {
        String name;
        Class<T> type;
        boolean required;
        @Ignore
        @Nullable
        @ToString.Exclude
        DataStructure<Component>.Property<T> prop;

        public Stream<Map.Entry<Dependency<T>, Component>> find(Component in) {
            return Stream.concat(Stream.of(in), in.components(type)).flatMap(x -> {
                if (name.isEmpty() || x.getName().equals(name))
                    return Stream.of(new AbstractMap.SimpleImmutableEntry<>(this, x));
                return empty();
            });
        }

        public boolean isSatisfied(Component by) {
            var wrap = Optional.ofNullable(by);
            if (!name.isEmpty())
                wrap = wrap.filter(c -> c.getName().equals(name));
            return wrap.filter(type::isInstance).isPresent();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Dependency)
                return hashCode() == other.hashCode();
            if (other instanceof Requires req)
                return name.isEmpty() && Arrays.asList(req.value()).contains(type);
            if (other instanceof Inject inj)
                return name.equals(inj.value()) && type.equals(inj.type());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, required);
        }
    }

    @Getter
    class Base extends Container.Base implements Component {
        protected boolean enabled = true;
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
        public Object addChildren(Object @NotNull ... children) {
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
        public final synchronized void initialize() {
            try {
                if (!testState(State.PreInit))
                    return;
                Log.at(Level.FINE, "Initializing " + this);

                injectDependencies();
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
        public final synchronized void lateInitialize() {
            if (!testState(State.Init) && !pushState(State.LateInit))
                return;
            runOnDependencies(Component::lateInitialize).join();
            $lateInitialize();
            runOnChildren(LifeCycle.class, LifeCycle::lateInitialize, it -> test(it, State.Init));

            pushState(State.Active);
        }

        @Override
        //@PostUpdate
        public final synchronized void tick() {
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
        public final synchronized void earlyTerminate() {
            pushState(State.EarlyTerminate);
            runOnChildren(LifeCycle.class, LifeCycle::earlyTerminate, it -> test(it, State.Active));
            $earlyTerminate();
        }

        @Override
        //@PreRemove @PreDestroy
        public final synchronized void terminate() {
            if (testState(State.PostTerminate))
                return;
            try {
                earlyTerminate();

                pushState(State.Terminate);
                $terminate();
                runOnChildren(LifeCycle.class, LifeCycle::terminate, it -> test(it, State.EarlyTerminate));

                pushState(State.PostTerminate);
                cleanupChildren();
                close();
            } catch (Throwable t) {
                Log.at(Level.WARNING, "Could not correctly terminate %s".formatted(this));
            }
        }

        private void cleanupChildren() {
            final Predicate<Object> isNotComponent = x -> !Component.class.isAssignableFrom(x.getClass());
            final var remove = streamChildren(Object.class).filter(isNotComponent).toArray();
            if (removeChildren(remove) != remove.length)
                Log.at(Level.WARNING, "Could not remove all children of %s".formatted(this));
        }

        private void injectDependencies() {
            dependencies().stream()
                    .flatMap(dependency -> dependency.find(this))
                    .flatMap(filter(Dependency::isSatisfied, (dep, comp) -> {
                        if (dep.isRequired())
                            throw new Constraint.UnmetError("Could not find a valid Component matching " + dep);
                    }))
                    .flatMap(flatMapA(dep -> Wrap.of(dep)
                            .map(Dependency::getProp)
                            .map(DataStructure.Property::getSetter)
                            .stream()))
                    .flatMap(filterA(func -> func.setAccessible(true),
                            func -> Log.at(Level.WARNING, "Unable to make setter accessible: " + func)))
                    .forEach(forEach(Invocable::silentAutoInvoke));
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
            record InitEntry(Component component, @Nullable CompletableFuture<?> future) {
            }

            if (getParent() == null)
                return CompletableFuture.completedFuture(null);
            final var wrap = action.wrap();
            final var entries = dependencies().stream()
                    .flatMap(dependency -> getParent().getChildren().stream()
                            .filter(e -> dependency.type.isAssignableFrom(e.getClass()))
                            .flatMap(cast(Component.class)))
                    .filter(c -> c.testState(State.PreInit))
                    .peek(c -> Log.at(Level.FINE, "Initializing dependency of %s first: %s".formatted(this, c)))
                    .map(c -> new InitEntry(c, CompletableFuture.supplyAsync(() -> {
                        wrap.accept(c);
                        return null;
                    })))
                    .toArray(InitEntry[]::new);
            var caller = StackTraceUtils.caller(1);
            var missing = dependencies().stream()
                    .filter(t -> Arrays.stream(entries)
                            .noneMatch(e -> e.component.testState(State.PreInit) // todo: this filter is probably wrong
                                    && t.type.isAssignableFrom(e.component.getClass())))
                    .map(dep -> dep.type.getCanonicalName())
                    .toList();
            if (!missing.isEmpty())
                Log.at(Level.WARNING, "Could not run on all dependencies\n\tat %s\n\tParent Module: %s\n\tEntries:\n\t\t- %s\n\tMissing Dependencies:\n\t\t- %s"
                        .formatted(caller, this, String.join("\n\t\t- ",
                                        Arrays.stream(entries).map(e -> e.component.toString()).toArray(String[]::new)),
                                String.join("\n\t\t- ", missing)));
            return CompletableFuture.allOf(Arrays.stream(entries)
                    .map(e -> e.future)
                    .toArray(CompletableFuture[]::new));
        }
    }

    @Data
    class Sub<Parent extends Component> extends Base {
        protected final Parent parent;

        @Override
        public final boolean isSubComponent() {
            return true;
        }

        @Override
        public final @Nullable Parent getParent() {
            return parent;
        }

        @Override
        public final Component.Base setParent(@Nullable Component parent) {
            return this; // do nothing, parent is final
        }
    }
}
