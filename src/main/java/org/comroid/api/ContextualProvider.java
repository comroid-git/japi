package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

@Experimental
public interface ContextualProvider {
    Iterable<Object> getContextMembers();

    static ContextualProvider create(Object... members) {
        final List<Object> underlying = Arrays.asList(members);
        return () -> underlying;
    }

    @NonExtendable
    default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
        return StreamSupport.stream(getContextMembers().spliterator(), false)
                .map(member -> member instanceof ContextualTypeProvider
                        ?
                        (memberType.isAssignableFrom(((ContextualTypeProvider<?>) member).getContextMemberType())
                                ? ((ContextualTypeProvider<?>) member).getFromContext()
                                : ((ContextualTypeProvider<?>) member).getFromContext(memberType).get())
                        : member)
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

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        @NonExtendable
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }
    }
}
