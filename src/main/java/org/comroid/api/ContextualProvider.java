package org.comroid.api;

import org.comroid.util.ReflectionHelper;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

import static org.comroid.util.ReflectionHelper.extendingClassesCount;

@Experimental
public interface ContextualProvider extends Specifiable<ContextualProvider> {
    static ContextualProvider create(Object... members) {
        return new Base(members);
    }

    Iterable<Object> getContextMembers();

    <T> Rewrapper<T> getFromContext(final Class<T> memberType);

    ContextualProvider plus(Object plus);

    @Override
    default <R extends ContextualProvider> Optional<R> as(Class<R> type) {
        if (isType(type))
            return Optional.ofNullable(type.cast(self().get()));
        return getFromContext(type).wrap();
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<T> memberType) throws NoSuchElementException {
        return requireFromContext(memberType, String.format("No member of type %s found", memberType));
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<T> memberType, String message) throws NoSuchElementException {
        return getFromContext(memberType).assertion(message);
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
            return getUnderlyingContextualProvider().getFromContext(memberType);
        }

        @Override
        default ContextualProvider plus(Object plus) {
            return getUnderlyingContextualProvider().plus(plus);
        }

        @Override
        @NonExtendable
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }
    }

    @Experimental
    interface This extends ContextualProvider {
        @Override
        default Iterable<Object> getContextMembers() {
            return Collections.singleton(this);
        }

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
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
    interface Member extends ContextualProvider {
        Object getFromContext();

        @Override
        default Iterable<Object> getContextMembers() {
            return Collections.singleton(getFromContext());
        }

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
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
        private final Set<Object> members = new HashSet<>();

        @Override
        public Collection<Object> getContextMembers() {
            return members;
        }

        protected Base(Object... initialMembers) {
            members.addAll(Arrays.asList(initialMembers));
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
        public final ContextualProvider plus(Object plus) {
            members.add(plus);
            return this;
        }
    }
}
