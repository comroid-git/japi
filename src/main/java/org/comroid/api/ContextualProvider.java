package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Experimental
public interface ContextualProvider {
    Iterable<Object> getContextMembers();

    static ContextualProvider create(Object... members) {
        final Set<Object> collect = Stream.of(members)
                .map(it -> it instanceof ContextualProvider
                        ? ((ContextualProvider) it).getContextMembers().spliterator()
                        : Collections.singletonList(it).spliterator())
                .flatMap(split -> StreamSupport.stream(split, false))
                .collect(Collectors.toSet());
        return () -> collect;
    }

    @Internal
    static <T> Object unwrapPossibleProvider(Class<T> memberType, Object member) {
        if (member instanceof ContextualTypeProvider
                && memberType.isAssignableFrom(((ContextualTypeProvider<?>) member).getContextMemberType()))
            return ((ContextualTypeProvider<?>) member).getFromContext();
        return member;
    }

    @NonExtendable
    default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
        return StreamSupport.stream(getContextMembers().spliterator(), false)
                .map(member -> unwrapPossibleProvider(memberType, member))
                .filter(memberType::isInstance)
                .findAny()
                .map(memberType::cast)
                .map(it -> (Rewrapper<T>) () -> it)
                .orElse(() -> null);
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<T> memberType) throws NoSuchElementException {
        return getFromContext(memberType).requireNonNull(() -> String.format("No member of type %s found", memberType));
    }

    default ContextualProvider plus(Object plus) {
        return create(this, plus);
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        @NonExtendable
        default Iterable<Object> getContextMembers() {
            ContextualProvider maybeUnderlying = getUnderlyingContextualProvider();
            if (maybeUnderlying == this)
                return getContextMembers();
            return maybeUnderlying.getContextMembers();
        }
    }
}
