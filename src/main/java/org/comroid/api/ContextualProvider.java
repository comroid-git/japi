package org.comroid.api;

import org.comroid.annotations.inheritance.MustExtend;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.comroid.util.StackTraceUtils.callerClass;

@Experimental
@MustExtend(ContextualProvider.Base.class)
public interface ContextualProvider extends Named, Specifiable<ContextualProvider> {
    @Deprecated
    default Iterable<Object> getContextMembers() {
        return Collections.unmodifiableSet(streamContextMembers().collect(Collectors.toSet()));
    }

    @Internal
    default @Nullable ContextualProvider getParentContext() {
        return null;
    }

    @Internal
    @NonExtendable
    default boolean isRoot() {
        return getParentContext() == null;
    }

    static ContextualProvider create(Object... members) {
        return new Base(Base.ROOT, callerClass(1), members);
    }

    static ContextualProvider create(ContextualProvider parent, Object... members) {
        return new Base((Base) parent, callerClass(1), members);
    }

    @Internal
    static String wrapContextStr(String subStr) {
        return "Context<" + subStr + ">";
    }

    default Stream<Object> streamContextMembers() {
        return StreamSupport.stream(getContextMembers().spliterator(), false);
    }

    @NonExtendable
    default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
        return () -> streamContextMembers()
                .filter(memberType::isInstance)
                .findFirst()
                .map(memberType::cast)
                .orElse(null);
    }

    @NonExtendable
    default ContextualProvider plus(Object plus) {
        return plus(callerClass(1).getSimpleName(), plus);
    }

    @NonExtendable
    default ContextualProvider plus(String name, Object plus) {
        return new Base((Base) this, name, plus);
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
            Class<? extends This> cls = getClass();
            if (cls == null)
                return "uninitialized context";
            return wrapContextStr(cls.getSimpleName());
        }

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
            return wrapContextStr(getFromContext().getClass().getSimpleName());
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
        @SuppressWarnings("ConstantConditions")
        public static final ContextualProvider.Base ROOT
                = new ContextualProvider.Base(null, "ROOT");
        protected final Set<Object> myMembers;
        private final Set<ContextualProvider> children;
        private final ContextualProvider parent;
        private final String name;

        @Override
        public final @Nullable ContextualProvider getParentContext() {
            return parent;
        }

        @Override
        public String getName() {
            return wrapContextStr(name);
        }

        protected Base(@NotNull ContextualProvider.Base parent, Object... initialMembers) {
            this(parent, callerClass(1).getSimpleName(), initialMembers);
        }

        @SuppressWarnings("NullableProblems")
        protected Base(@NotNull ContextualProvider.Base parent, String name, Object... initialMembers) {
            this.myMembers = new HashSet<>();
            this.children = new HashSet<>();
            if (!isRoot())
                parent.children.add(this);

            this.parent = name.equals("ROOT") && callerClass(1).equals(ContextualProvider.Base.class)
                    ? parent
                    : Objects.requireNonNull(parent);
            this.name = name;

            myMembers.addAll(Arrays.asList(initialMembers));
            myMembers.add(this);
        }

        @Override
        public final Stream<Object> streamContextMembers() {
            return Stream.of(
                    Stream.of(parent).flatMap(ContextualProvider::streamContextMembers),
                    myMembers.stream()
            );
        }

        @Override
        public final boolean addToContext(Object plus) {
            return myMembers.add(plus);
        }
    }
}
