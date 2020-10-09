package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;

@Experimental
public interface ContextualTypeProvider<T> extends ContextualProvider {
    @NotNull T getFromContext();

    default Class<T> getContextMemberType() {
        return Polyfill.uncheckedCast(getFromContext().getClass());
    }

    @Override
    default Iterable<Object> getContextMembers() {
        return Collections.singleton(getFromContext());
    }

    @Override
    default <R> Optional<R> getFromContext(final Class<R> memberType) {
        return memberType.isAssignableFrom(getContextMemberType())
                ? Optional.of(memberType.cast(getFromContext()))
                : Optional.empty();
    }

    interface This<T> extends ContextualTypeProvider<T> {
        @Override
        @NotNull
        default T getFromContext() {
            return Polyfill.uncheckedCast(this);
        }

        @Override
        default Class<T> getContextMemberType() {
            return Polyfill.uncheckedCast(getClass());
        }
    }

    interface Underlying<T> extends ContextualTypeProvider<T>, ContextualProvider.Underlying {
        @Override
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }

        @Override
        @NotNull
        default T getFromContext() {
            return getUnderlyingContextualProvider().getFromContext();
        }

        @Override
        ContextualTypeProvider<T> getUnderlyingContextualProvider();
    }
}
