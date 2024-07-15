package org.comroid.api.tree;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import org.comroid.annotations.Ignore;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.EnabledState;
import org.comroid.api.attr.Named;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.func.exc.ThrowingConsumer;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Bitmask;
import org.comroid.api.func.util.Cache;
import org.comroid.api.info.Constraint;
import org.comroid.api.info.Log;
import org.comroid.api.info.Maintenance;
import org.comroid.api.java.StackTraceUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.*;
import static java.util.stream.Stream.*;
import static org.comroid.api.Polyfill.*;
import static org.comroid.api.func.util.Streams.*;
import static org.comroid.api.text.Capitalization.*;

@Ignore
public interface Component extends Container, LifeCycle, Tickable, EnabledState, Named {
    Maintenance.Inspection MI_MissingDependency = Maintenance.Inspection.builder()
            .name("Component missing Dependency")
            .format("Component %s is missing Dependency %s")
            .description("A required dependency is missing")
            .build();

    default boolean isActive() {
        return getCurrentState() == State.Active;
    }

    static Set<Dependency> dependencies(Class<? extends Component> type) {
        return Cache.get("dependencies of " + type.getCanonicalName(), () -> {
            var struct = DataStructure.of(type);
            //noinspection unchecked,rawtypes            suck my dick, compiler
            return Stream.concat(
                            struct.getProperties().stream()
                                    .flatMap(prop -> prop.streamAnnotations(Inject.class)
                                            .map(Annotations.Result::getAnnotation)
                                            .map(inject -> new Dependency(
                                                    Optional.ofNullable(inject.value())
                                                            .filter(not(String::isEmpty))
                                                            .orElseGet(prop::getName),
                                                    Optional.ofNullable((Class) inject.type())
                                                            .filter(not(cls -> cls.equals(Component.class)))
                                                            .orElseGet(() -> prop.getType().getTargetClass()),
                                                    !prop.isAnnotationPresent(Nullable.class) && (inject.required()
                                                            || prop.isAnnotationPresent(NotNull.class)),
                                                    (DataStructure<? extends Component>.Property<?>) prop))),
                            Annotations.findAnnotations(Requires.class, type)
                                    .flatMap(requires -> Arrays.stream(requires.getAnnotation().value()))
                                    .map(cls -> new Dependency("", cls, true, null)))
                    .collect(Collectors.toUnmodifiableSet());
        });
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

    default Set<Dependency> dependencies() {
        return dependencies(getClass());
    }

    State getCurrentState();

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

    default boolean testState(State state) {
        return Bitmask.isFlagSet(getCurrentState().mask, state);
    }

    default void start() {
        initialize();
    }

    default void stop() {
        terminate();
    }

    //static List<Class<? extends Component>> includes()

    @Override
    default void closeSelf() throws Exception {
        terminate();
    }

    enum State implements Bitmask.Attribute<State> {
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
    @Retention(RetentionPolicy.RUNTIME) @interface Requires {
        Class<? extends Component>[] value();
    }

    /**
     * declare field injection
     * occurs before initialization
     * if all are defaults, then name and type from field are used
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD }) @interface Inject {
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
    @ToString(of = { "type", "name" })
    @EqualsAndHashCode(of = { "type", "name" })
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Dependency {
        String  name;
        Class<?> type;
        boolean required;
        @Ignore
        @Nullable
        DataStructure<? extends Component>.Property<?> prop;
    }

    @Getter
    @Ignore
    class Base extends Container.Base implements Component {
        protected boolean enabled       = true;
        private   State   currentState  = State.PreInit;
        private   State   previousState = State.PreInit;
        private @Setter
        @Nullable Component parent;
        private @Setter
        @Nullable String  name          = "%s#%x".formatted(StackTraceUtils.lessSimpleName(getClass()), hashCode());

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
                var count = runOnDependencies(Component::initialize);

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

        protected void $tick() {
        }

        @Override
        public final synchronized void lateInitialize() {
            if (!testState(State.Init) && !pushState(State.LateInit))
                return;
            var count = runOnDependencies(Component::lateInitialize);
            $lateInitialize();
            runOnChildren(LifeCycle.class, LifeCycle::lateInitialize, it -> test(it, State.Init));

            pushState(State.Active);
        }

        private static <T> boolean test(T it, State state) {
            return it instanceof Component && ((Component) it).testState(state);
        }

        @Override
        public final synchronized void earlyTerminate() {
            pushState(State.EarlyTerminate);
            runOnChildren(LifeCycle.class, LifeCycle::earlyTerminate, it -> test(it, State.Active));
            $earlyTerminate();
        }

        protected void $earlyTerminate() {
        }

        protected void $lateInitialize() {
        }

        private void cleanupChildren() {
            final Predicate<Object> isNotComponent = x -> !Component.class.isAssignableFrom(x.getClass());
            final var remove = streamChildren(Object.class).filter(isNotComponent).toArray();
            if (removeChildren(remove) != remove.length)
                Log.at(Level.WARNING, "Could not remove all children of %s".formatted(this));
        }

        protected void $terminate() {
        }

        private void injectDependencies() {
            dependencies().stream()
                    .filter(dep -> dep.prop != null && dep.prop.canSet())
                    .<Map.Entry<Dependency, Component>>flatMap(dep -> {
                        var results = components(dep.type).toList();
                        if (results.size() > 1) {
                            final var names = dep.name.isEmpty()
                                              ? Stream.concat(Stream.of(dep.prop.getName()), dep.prop.getAliases().stream())
                                                      .collect(Collectors.toSet())
                                              : Set.of(dep.name);
                            var byName = results.stream()
                                    .filter(it -> names.stream().anyMatch(alias -> equalsIgnoreCase(alias, it.getName())))
                                    .toList();
                            if (byName.isEmpty()) {
                                Log.at(Level.WARNING, "Exact name match yielded no results; attempting to find with contains()");
                                byName = results.stream()
                                        .filter(it -> names.stream()
                                                .map(String::toLowerCase)
                                                .anyMatch(alias -> it.getName().toLowerCase().contains(alias)))
                                        .toList();
                            }
                            results = byName;
                        }
                        if (results.isEmpty()) {
                            MI_MissingDependency.new CheckResult(this, this, dep);
                            if (dep.isRequired())
                                throw new Constraint.UnmetError("Unmet required " + dep);
                            else return empty();
                        }
                        var result = results.get(0);
                        if (results.size() > 1)
                            Log.at(Level.WARNING, "More than one result for " + dep + "; using " + result);
                        return Stream.of(new AbstractMap.SimpleImmutableEntry<>(dep, result));
                    })
                    .forEach(e -> Wrap.of(e.getKey())
                            .map(Dependency::getProp)
                            .map(DataStructure.Property::getSetter)
                            .filter(func -> {
                                var success = func.makeAccessible();
                                if (!success)
                                    Log.at(Level.WARNING, "Unable to make setter accessible: " + func);
                                return success;
                            })
                            .ifPresentOrElseThrow(func -> func.invokeSilent(Component.Base.this, e.getValue()),
                                                  () -> new AssertionError("property was not settable")));
        }

        protected void $initialize() {
        }

        private boolean pushState(State state) {
            if (currentState == state)
                return false; // avoid pushing same state twice
            previousState = currentState;
            currentState = state;
            Log.at(Level.FINE, getName() + " changed into state: " + currentState);
            return true;
        }

        private long runOnDependencies(final ThrowingConsumer<Component, Throwable> action) {
            if (getParent() == null)
                return 0L;
            final var wrap = action.wrap();
            return dependencies().stream()
                    .filter(dep -> dep.prop != null)
                    .map(dep -> dep.prop.getFrom(this))
                    .flatMap(cast(Component.class))
                    .filter(c -> c.testState(State.PreInit))
                    .peek(c -> Log.at(Level.FINE, "Running %s on dependency of %s first: %s".formatted(action, this, c)))
                    .peek(wrap)
                    .count();
        }

        @Override
        public void stop() {
            terminate();
        }
    }

    @Data
    class Sub<Parent extends Component> extends Base {
        protected final Parent parent;

        @Override
        public final @Nullable Parent getParent() {
            return parent;
        }

        @Override
        public final Component.Base setParent(@Nullable Component parent) {
            return this; // do nothing, parent is final
        }

        @Override
        public final boolean isSubComponent() {
            return true;
        }
    }
}
