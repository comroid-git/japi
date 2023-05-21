package org.comroid.api;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Container extends UncheckedCloseable, SelfCloseable {
    Object addChildren(Object... children);
    Set<Object> getChildren();

    default <T> Stream<T> streamChildren(Class<T> type) {
        return getChildren().stream().filter(type::isInstance).map(type::cast);
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
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Base implements Container {
        private final Set<Object> children;

        public Base(Object... children) {
            this.children = new HashSet<>(Set.of(children));
        }

        public Object addChildren(Object... children) {
            this.children.addAll(List.of(children));
            return this;
        }

        @Override
        @SneakyThrows
        public final void close() {
            final List<Throwable> errors = Stream.concat(streamChildren(AutoCloseable.class), Stream.of(this::closeSelf)
                    ).parallel()
                    .filter(Objects::nonNull)
                    .flatMap(closeable -> {
                        try {
                            closeable.close();
                        } catch (Throwable e) {
                            return Stream.of(e);
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toUnmodifiableList());
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
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Delegate<S extends SelfCloseable> extends Base implements Owned {
        private final @NonNull S owner;

        @Override
        public S addChildren(Object... children) {
            super.addChildren(children);
            return owner;
        }

        public Delegate(@NonNull S owner, Object... children) {
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
