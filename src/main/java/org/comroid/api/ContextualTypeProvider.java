package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@Experimental
public interface ContextualTypeProvider<T> extends ContextualProvider {
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

    @Override
    @NonExtendable
    default <R> Rewrapper<R> getFromContext(final Class<R> memberType) {
        return memberType.isAssignableFrom(getContextMemberType())
                ? () -> memberType.cast(getFromContext())
                : () -> null;
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
        @NonExtendable
        default @NotNull T getFromContext() {
            return getUnderlyingContextualProvider().getFromContext();
        }

        @Override
        ContextualTypeProvider<T> getUnderlyingContextualProvider();
    }
}
