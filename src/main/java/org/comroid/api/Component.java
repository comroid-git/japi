package org.comroid.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.comroid.api.info.Log;
import org.comroid.util.Bitmask;
import org.comroid.util.StackTraceUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Stream;

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

    default BackgroundTask<Component> execute(ScheduledExecutorService scheduler, Duration tickRate) {
        initialize();
        return new BackgroundTask<>(this, Component::tick, tickRate.toMillis(), scheduler);
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
        //@PreRemove @PreDestroy
        public final void initialize() {
            try {
                if (!test(this, State.PreInit))
                    return;
                runOnChildren(Initializable.class, Initializable::initialize, it -> test(it, State.PreInit));
                $initialize();
                pushState(State.LateInit);

                lateInitialize();
            } catch (InitFailed ife) {
                Log.at(Level.SEVERE, "Could not initialize "+getName()+"; " + ife.getMessage());
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
            return !(it instanceof Component)||Bitmask.isFlagSet(((Component) it).getCurrentState().mask, state);
        }

        private boolean pushState(State state) {
            if (currentState == state)
                return false; // avoid pushing same state twice
            previousState = currentState;
            currentState = state;
            Log.at(Level.FINE, getName() + " changed into state: " + currentState);
            return true;
        }
    }
}
