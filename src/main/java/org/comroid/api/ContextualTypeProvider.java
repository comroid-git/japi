package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@Experimental
public interface ContextualTypeProvider<T> extends ContextualProvider {
    @Override
    default <O> Rewrapper<O> getFromContext(final Class<O> memberType) {
        if (getContextMemberType().isAssignableFrom(memberType))
            return () -> Polyfill.uncheckedCast(getFromContext());
        return Rewrapper.empty();
    }

    @Override
    default ContextualProvider plus(Object plus) {
        return ContextualProvider.create(getFromContext()).plus(plus);
    }

    @NotNull T getFromContext();

    @NonExtendable
    default Class<T> getContextMemberType() {
        return Polyfill.uncheckedCast(getFromContext().getClass());
    }

    @Override
    @NonExtendable
    default Iterable<Object> getContextMembers() {
        return Collections.singleton(getFromContext());
    }

    interface This<T> extends ContextualTypeProvider<T> {
        @Override
        @NonExtendable
        default @NotNull T getFromContext() {
            return Polyfill.uncheckedCast(this);
        }

        @Override
        @NonExtendable
        default Class<T> getContextMemberType() {
            return Polyfill.uncheckedCast(getClass());
        }
    }

    interface Underlying<T> extends ContextualTypeProvider<T>, ContextualProvider.Underlying {
        @Override
        @NonExtendable
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }

        @Override
        default <O> Rewrapper<O> getFromContext(final Class<O> memberType) {
            return getUnderlyingContextualProvider().getFromContext(memberType);
        }

        @Override
        default ContextualProvider plus(Object plus) {
            return getUnderlyingContextualProvider().plus(plus);
        }

        @Override
        @NonExtendable
        default @NotNull T getFromContext() {
            return getUnderlyingContextualProvider().getFromContext();
        }

        @Override
        ContextualTypeProvider<T> getUnderlyingContextualProvider();
    }
}
