package org.comroid.api;

import org.comroid.util.StackTraceUtils;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Experimental
public interface ContextualProvider extends Named, Specifiable<ContextualProvider> {
    @Deprecated
    default Iterable<Object> getContextMembers() {
        return Collections.unmodifiableSet(streamContextMembers().collect(Collectors.toSet()));
    }

    default Stream<Object> streamContextMembers() {
        return StreamSupport.stream(getContextMembers().spliterator(), false);
    }

    @Internal
    default @Nullable ContextualProvider getParentContext() {
        return null;
    }

    @Internal
    default boolean isRoot() {
        return getParentContext() == null;
    }

    static ContextualProvider create(Object... members) {
        return new Base(members);
    }

    <T> Rewrapper<T> getFromContext(final Class<T> memberType);

    default ContextualProvider plus(Object plus) {
        return plus(StackTraceUtils.callerClass(1).getSimpleName(), plus);
    }

    default ContextualProvider plus(String newName, Object plus) {
        throw new AbstractMethodError();
    }

    default boolean addToContext(Object plus) {
        return false;
    }

    @Override
    default <R extends ContextualProvider> Optional<R> as(Class<R> type) {
        if (isType(type))
            return Optional.ofNullable(type.cast(self().get()));
        return getFromContext(type).wrap();
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<T> memberType) throws NoSuchElementException {
        return requireFromContext(memberType, String.format("No member of type %s found in %s", memberType, this));
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<T> memberType, String message) throws NoSuchElementException {
        return getFromContext(memberType).assertion(String.format("<%s => %s>", this, message));
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        @Nullable
        default ContextualProvider getParentContext() {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.getParentContext();
        }

        @Override
        default String getName() {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.getName();
        }

        @Override
        default Stream<Object> streamContextMembers() {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.streamContextMembers();
        }

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.getFromContext(memberType);
        }

        @Override
        default ContextualProvider plus(String name, Object plus) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.plus(name, plus);
        }

        @Override
        default boolean addToContext(Object plus) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.addToContext(plus);
        }
    }

    @Experimental
    interface This<T> extends ContextualProvider {
        @Override
        default String getName() {
            return ContextualProvider.Base.wrapContextStr(getClass().getSimpleName());
        }

        @Override
        default Iterable<Object> getContextMembers() {
            return Collections.singleton(this);
        }

        @Override
        default Stream<Object> streamContextMembers() {
            return Stream.of(this);
        }

        @Override
        default <R> Rewrapper<R> getFromContext(final Class<R> memberType) {
            if (memberType.isAssignableFrom(getClass()))
                return () -> Polyfill.uncheckedCast(this);
            return Rewrapper.empty();
        }

        @Override
        default ContextualProvider plus(Object plus) {
            return create(this, plus);
        }
    }

    @Experimental
    interface Member<T> extends ContextualProvider {
        T getFromContext();

        @Override
        default String getName() {
            return ContextualProvider.Base.wrapContextStr(getFromContext().getClass().getSimpleName());
        }

        @Override
        default Iterable<Object> getContextMembers() {
            return Collections.singleton(getFromContext());
        }

        @Override
        default Stream<Object> streamContextMembers() {
            return Stream.of(getFromContext());
        }

        @Override
        default <R> Rewrapper<R> getFromContext(final Class<R> memberType) {
            if (memberType.isAssignableFrom(getFromContext().getClass()))
                return () -> Polyfill.uncheckedCast(getFromContext());
            return Rewrapper.empty();
        }

        @Override
        default ContextualProvider plus(Object plus) {
            return create(getFromContext(), plus);
        }
    }

    @Internal
    class Base implements ContextualProvider {
        protected final Set<Object> myMembers = new HashSet<>();
        private final ContextualProvider parent;
        private final String name;

        @Override
        public @Nullable ContextualProvider getParentContext() {
            return parent;
        }

        @Override
        public Collection<Object> getContextMembers() {
            return myMembers;
        }

        @Override
        public Stream<Object> streamContextMembers() {
            return Stream.of(
                    Stream.of(parent).flatMap(ContextualProvider::streamContextMembers),
                    myMembers.stream()
            );
        }

        @Override
        public String getName() {
            return wrapContextStr(name);
        }

        @Deprecated
        protected Base(Object... initialMembers) {
            this((ContextualProvider) null, initialMembers);
        }

        protected Base(ContextualProvider parent, Object... initialMembers) {
            this(parent, StackTraceUtils.callerClass(1).getSimpleName(), initialMembers);
        }

        protected Base(String name, Object... initialMembers) {
            this(null, name, initialMembers);
        }

        protected Base(ContextualProvider parent, String name, Object... initialMembers) {
            this.parent = parent;
            this.name = name;
            myMembers.addAll(Arrays.asList(initialMembers));
            myMembers.add(this);
        }

        @Internal
        private static String wrapContextStr(String subStr) {
            return "Context<" + subStr + ">";
        }

        @Override
        public final <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
            return () -> getContextMembers()
                    .stream()
                    .filter(memberType::isInstance)
                    .findAny()
                    .map(memberType::cast)
                    .orElse(null);
        }

        @Override
        public final ContextualProvider plus(String name, Object plus) {
            ContextualProvider.Base base = new ContextualProvider.Base(name, myMembers.toArray());
            if (base.addToContext(plus))
                return base;
            return null;
        }

        @Override
        public boolean addToContext(Object plus) {
            return myMembers.add(plus);
        }
    }
}
