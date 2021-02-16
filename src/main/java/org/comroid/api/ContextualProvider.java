package org.comroid.api;

import org.comroid.util.StackTraceUtils;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Experimental
public interface ContextualProvider extends Named, Specifiable<ContextualProvider> {
    Iterable<Object> getContextMembers();

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

    default boolean add(Object plus) {
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
        return getFromContext(memberType).assertion(message);
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        default String getName() {
            return getUnderlyingContextualProvider().getName();
        }

        @Override
        @NonExtendable
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
            return getUnderlyingContextualProvider().getFromContext(memberType);
        }

        @Override
        default ContextualProvider plus(Object plus) {
            return getUnderlyingContextualProvider().plus(plus);
        }

        @Override
        default boolean add(Object plus) {
            return getUnderlyingContextualProvider().add(plus);
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
        protected final Set<Object> members = new HashSet<>();
        private final String name;

        @Override
        public Collection<Object> getContextMembers() {
            return members;
        }

        @Override
        public String getName() {
            return wrapContextStr(name);
        }

        protected Base(Object... initialMembers) {
            this(StackTraceUtils.callerClass(1).getSimpleName(), initialMembers);
        }

        protected Base(String name, Object... initialMembers) {
            this.name = name;
            members.addAll(Arrays.asList(initialMembers));
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
            ContextualProvider.Base base = new ContextualProvider.Base(name, members.toArray());
            base.add(plus);
            return base;
        }

        @Override
        public boolean add(Object plus) {
            return members.add(plus);
        }
    }
}
