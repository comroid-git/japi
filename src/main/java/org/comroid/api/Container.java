package org.comroid.api;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Container extends UncheckedCloseable, SelfCloseable {
    Object addChildren(Object... children);
    Set<Object> getChildren();

    default Stream<Object> streamOwnChildren() {
        return Stream.empty();
    }

    default <T> Stream<T> streamChildren(Class<T> type) {
        return Stream.concat(getChildren().stream(), streamOwnChildren())
                .filter(type::isInstance)
                .map(type::cast);
    }

    static Container of(Object... children) {
        return new Base(children);
    }

    private static Exception makeException(List<Throwable> errors) {
        return new Exception(String.format("%d unexpected %s occurred",
                errors.size(),
                Polyfill.plural(errors, "exception", "+s")),
                null,
                true, false){};
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    class Base implements Container {
        final Set<Object> children;
        protected boolean closed;

        public Base(Object... children) {
            this.children = new HashSet<>(Set.of(children));
        }

        public Object addChildren(@Nullable Object... children) {
            Stream.of(children)
                    .filter(Objects::nonNull)
                    .forEach(this.children::add);
            return this;
        }

        @Override
        @SneakyThrows
        public final void close() {
            final List<Throwable> errors = Stream.concat(Stream.concat(streamChildren(AutoCloseable.class), moreMembers()), Stream.of(this::closeSelf))
                    .parallel()
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(this::equals))
                    .flatMap(closeable -> {
                        try {
                            closeable.close();
                        } catch (Throwable e) {
                            return Stream.of(e);
                        }
                        return Stream.empty();
                    })
                    .distinct()
                    .toList();
            closed = true;
            if (errors.isEmpty())
                return;
            if (errors.size() == 1)
                throw errors.get(0);
            throw errors.stream().collect(
                    ()->Container.makeException(errors),
                    Throwable::addSuppressed,
                    (l, r) -> Arrays.stream(r.getSuppressed()).forEachOrdered(l::addSuppressed));
        }

        @Override
        public void closeSelf() throws Exception {
        }

        protected Stream<AutoCloseable> moreMembers() {
            return Stream.empty();
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Delegate<S extends SelfCloseable> extends Base implements Owned {
        private final @NotNull S owner;

        @Override
        public S addChildren(Object... children) {
            super.addChildren(children);
            return owner;
        }

        public Delegate(@NotNull S owner, Object... children) {
            super(children);
            this.owner = owner;
        }

        @Override
        @OverrideOnly
        @SuppressWarnings("RedundantThrows")
        public void closeSelf() throws Exception {
            owner.closeSelf();
        }
    }
}
