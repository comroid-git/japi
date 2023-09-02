package org.comroid.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.persistence.PostLoad;
import javax.persistence.PreRemove;
import java.util.stream.Stream;

public interface Component extends Container, LifeCycle {
    @Nullable Component getParent();

    @Contract("_ -> this")
    Component setParent(@Nullable Component parent);

    default <T extends Component> Stream<T> components(@Nullable Class<T> type) {
        return streamChildren(type);
    }

    default <T extends Component> Rewrapper<T> component(@Nullable Class<T> type) {
        return () -> components(type).findAny().orElse(null);
    }

    @Data
    class Base extends Container.Base implements Component {
        private @Nullable Component parent;

        @PostLoad
        @PostConstruct
        @Override
        public final void initialize() {
            runOnChildren(Initializable.class, Initializable::initialize);
            $initialize();
            lateInitialize();
        }

        @Override
        public final void lateInitialize() {
            runOnChildren(LifeCycle.class, LifeCycle::lateInitialize);
            $lateInitialize();
        }

        @Override
        public final void earlyTerminate() {
            runOnChildren(LifeCycle.class, LifeCycle::earlyTerminate);
            $earlyTerminate();
        }

        @PreRemove
        @PreDestroy
        @Override
        public final void terminate() {
            earlyTerminate();
            $terminate();
            runOnChildren(LifeCycle.class, LifeCycle::terminate);
        }

        protected final void $initialize() {
        }

        protected final void $lateInitialize() {
        }

        protected final void $earlyTerminate() {
        }

        protected final void $terminate() {
        }
    }
}
