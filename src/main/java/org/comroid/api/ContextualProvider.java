package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Experimental
public interface ContextualProvider {
    Iterable<Object> getContextMembers();

    static ContextualProvider combine(ContextualProvider... providers) {
        final List<Object> underlying = Arrays.asList(providers);
        return () -> underlying;
    }

    default <T> Optional<T> getFromContext(final Class<T> memberType) {
        return StreamSupport.stream(getContextMembers().spliterator(), false)
                .map(member -> member instanceof ContextualTypeProvider
                        && memberType.isAssignableFrom(((ContextualTypeProvider<?>) member).getContextMemberType())
                        ? ((ContextualTypeProvider<?>) member).getFromContext()
                        : member)
                .filter(memberType::isInstance)
                .findAny()
                .map(memberType::cast);
    }

    default <T> @NotNull T requireFromContext(final Class<T> memberType) throws NoSuchElementException {
        return getFromContext(memberType).orElseThrow(() -> new NoSuchElementException(String.format("No member of type %s found", memberType)));
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }
    }
}
