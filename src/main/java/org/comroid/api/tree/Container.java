package org.comroid.api.tree;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.Ignore;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.EnabledState;
import org.comroid.api.attr.Owned;
import org.comroid.api.func.Specifiable;
import org.comroid.api.func.exc.ThrowingConsumer;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Streams;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.comroid.api.func.util.Streams.*;

public interface Container extends Stoppable, SelfCloseable, Specifiable<Container> {
    static Container of(Object... children) {
        return new Base(children);
    }

    @ApiStatus.Internal
    Set<Object> getChildren();

    Object addChildren(@Nullable Object @NotNull ... children);

    int removeChildren(@Nullable Object @NotNull ... children);

    void clearChildren();

    default <T> Stream<T> streamChildren(@Nullable Class<? super T> type) {
        return Stream.concat(getChildren().stream(), streamOwnChildren())
                .flatMap(Streams.cast(type))
                .map(Polyfill::uncheckedCast);
    }

    default Stream<Object> streamOwnChildren() {
        return Stream.empty();
    }

    default <T> Stream<T> children(@Nullable Class<? super T> type) {
        return this.streamChildren(type);
    }

    default <T> Wrap<T> child(@Nullable Class<? super T> type) {
        return () -> Polyfill.uncheckedCast(this.children(type).findAny().orElse(null));
    }

    private static Exception makeException(List<Throwable> errors) {
        return new Exception(String.format("%d unexpected %s occurred",
                errors.size(),
                Polyfill.plural(errors, "exception", "+s")),
                null,
                true, false) {
        };
    }

    class Base implements Container, Reloadable {
        @Ignore @Getter final Set<Object>                              children;
        @Ignore @Getter
        private final         AtomicReference<CompletableFuture<Void>> closed = new AtomicReference<>(new CompletableFuture<>());

        public Base(Object... children) {
            this.children = new HashSet<>(Set.of(children));
        }

        @Ignore
        public boolean isClosed() {
            return closed.get().isDone();
        }

        @Contract("_ -> this")
        public <T> Object addChild(@Nullable T it) {
            return Polyfill.uncheckedCast(addChildren(it));
        }

        @Contract("_ -> this")
        public Object addChildren(@Nullable Object @NotNull ... children) {
            Stream.of(children)
                    .filter(Objects::nonNull)
                    .forEach(this.children::add);
            return this;
        }

        @Override
        public int removeChildren(Object @NotNull ... children) {
            return (int) Stream.of(children)
                    .filter(this.children::remove)
                    .count();
        }

        @Override
        public void clearChildren() {
            children.clear();
        }

        @Override
        @SneakyThrows
        public void start() {
            runOnChildren(Startable.class, Startable::start, $ -> true);
            setClosed(false);
        }

        @SafeVarargs
        @SneakyThrows
        protected final <T> void runOnChildren(Class<T> type, ThrowingConsumer<T, Throwable> task, Predicate<T> test, T... extra) {
            final List<Throwable> errors = streamChildren(type)
                    .collect(append(moreMembers().flatMap(cast(type))))
                    .collect(append(extra))
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(this::equals))
                    // extra enabled check for EnabledState objects
                    .filter(it -> !(it instanceof EnabledState) || ((EnabledState) it).isEnabled())
                    .filter(test)
                    .flatMap(it -> {
                        try {
                            task.accept(it);
                        } catch (Throwable e) {
                            return Stream.of(e);
                        }
                        return Stream.empty();
                    })
                    .distinct()
                    .toList();
            if (errors.isEmpty())
                return;
            if (errors.size() == 1)
                throw errors.get(0);
            throw errors.stream().collect(
                    () -> Container.makeException(errors),
                    Throwable::addSuppressed,
                    (l, r) -> Arrays.stream(r.getSuppressed()).forEachOrdered(l::addSuppressed));
        }

        protected Stream<AutoCloseable> moreMembers() {
            return Stream.empty();
        }

        public boolean setClosed(boolean state) {
            return Polyfill.updateBoolState(isClosed(), state,
                    () -> closed.get().complete(null),
                    () -> closed.set(new CompletableFuture<>()));
        }

        @Override
        @SneakyThrows
        public final void close() {
            runOnChildren(AutoCloseable.class, AutoCloseable::close, $ -> true, this::closeSelf);
            setClosed(true);
        }

        @Override
        public void closeSelf() throws Exception {
        }

        public CompletableFuture<Void> onClose() {
            return closed.get();
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Delegate<S extends SelfCloseable> extends Base implements Owned {
        private final @NotNull S owner;

        public Delegate(@NotNull S owner, Object... children) {
            super(children);
            this.owner = owner;
        }

        @Override
        public S addChildren(Object @NotNull ... children) {
            super.addChildren(children);
            return owner;
        }

        @Override
        @OverrideOnly
        @SuppressWarnings("RedundantThrows")
        public void closeSelf() throws Exception {
            owner.closeSelf();
        }
    }
}
