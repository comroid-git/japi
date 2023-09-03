package org.comroid.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.comroid.util.Bitmask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.persistence.PostLoad;
import javax.persistence.PreRemove;
import java.util.Optional;
import java.util.stream.Stream;

import static org.comroid.util.StackTraceUtils.lessSimpleName;

public interface Component extends Container, LifeCycle, Tickable, Named {
    State getCurrentState();

    @Nullable Component getParent();

    @Contract("_ -> this")
    Component setParent(@Nullable Component parent);

    default <T extends Component> Stream<T> components(@Nullable Class<T> type) {
        return streamChildren(type);
    }

    default <T extends Component> Rewrapper<T> component(@Nullable Class<T> type) {
        return () -> components(type).findAny().orElse(null);
    }

    enum State implements BitmaskAttribute<State> {
        PreInit,
        Init,
        LateInit(Init),

        Active,

        EarlyTerminate,
        Terminate(EarlyTerminate),
        PostTerminate;

        private final int mask;

        State(State... ext) {
            this.mask = Bitmask.nextFlag() | Bitmask.combine(ext);
        }

        public int getAsInt() {
            return mask;
        }
    }

    @Log
    @Getter
    @RequiredArgsConstructor
    class Base extends Container.Base implements Component {
        private State currentState = State.PreInit;
        private State previousState = State.PreInit;
        private @Setter @Nullable Component parent;
        private @Setter @Nullable String name = Optional.ofNullable(parent)
                .map(parent -> parent.getName()+'.'+ lessSimpleName(getClass())+'#'+hashCode())
                .orElseGet(() -> lessSimpleName(getClass()));

        public boolean isActive() {
            return currentState == State.Active;
        }
        
        @Override
        @PostLoad
        @PostConstruct
        public final void initialize() {
            try {
                pushState(State.Init);
                runOnChildren(Initializable.class, Initializable::initialize);
                $initialize();

                lateInitialize();

                pushState(State.Active);
            } catch (InitFailed ife) {
                log.severe("Could not initialize "+getName()+"; " + ife.getMessage());
                terminate();
            }
        }

        @Override
        public final void lateInitialize() {
            pushState(State.LateInit);
            runOnChildren(LifeCycle.class, LifeCycle::lateInitialize);
            $lateInitialize();
        }

        @Override
        public void tick() {
            $tick();
            runOnChildren(Tickable.class, Tickable::tick);
        }

        @Override
        public final void earlyTerminate() {
            pushState(State.EarlyTerminate);
            runOnChildren(LifeCycle.class, LifeCycle::earlyTerminate);
            $earlyTerminate();
        }

        @Override
        @PreRemove
        @PreDestroy
        public final void terminate() {
            earlyTerminate();

            pushState(State.Terminate);
            $terminate();
            runOnChildren(LifeCycle.class, LifeCycle::terminate);

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

        private void pushState(State state) {
            if (currentState == state)
                return; // avoid pushing same state twice
            previousState = currentState;
            currentState = state;
        }
    }
}
