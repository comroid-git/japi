package org.comroid.api.func.ext;

import lombok.Getter;
import lombok.SneakyThrows;
import org.comroid.annotations.inheritance.MustExtend;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.LoggerCarrier;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.Serializer;
import org.comroid.api.func.exc.ThrowingSupplier;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.RootContextSource;
import org.comroid.api.java.ReflectionHelper;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.*;
import static org.comroid.api.java.StackTraceUtils.*;

/**
 * Structure that allows passing many system-wide values using a single parameter,
 * such as Executors, Serializers and other Adapters.
 * <p>
 * It is not advised to create a new context yourself, instead it is recommended to use the
 * {@linkplain Context#root() default Root context} and create derivates from that.
 * The default root context is initialized and set up by creating a resource named
 * {@code org/comroid/api/context.properties}, which contains all implementation classes
 * that are supposed to be contained in the Root context. The keys in this properties-file are meaningless,
 * but of course you can not duplicate keys.
 * <p>
 * A standard root context setup may look like this: {@code
 * serialization=org.comroid.uniform.adapter.json.fastjson.FastJSONLib
 * http=org.comroid.restless.adapter.java.JavaHttpAdapter
 * }
 * An instance of the implementation class is obtained using {@link ReflectionHelper#obtainInstance(Class, Object...)}.
 */
@Experimental
@MustExtend(Context.Base.class)
public interface Context extends Named, Convertible, LoggerCarrier {
    static Context root() {
        return Base.ROOT.get();
    }

    static <T> @Nullable T get(Class<T> type) {
        return wrap(type).get();
    }

    static <T> Wrap<T> wrap(Class<T> type) {
        return root().getFromContext(type);
    }

    static <T> Stream<? extends T> stream(Class<T> type) {
        return root().streamContextMembers(true, type);
    }

    /**
     * @deprecated Use {@link Context#root()} instead of creating new contexts
     */
    @Deprecated(forRemoval = true)
    static Context create(Object... members) {
        return new Base(Base.ROOT, callerClass(1), members);
    }

    /**
     * @deprecated Use {@link Context#root()} instead of creating new contexts
     */
    @Deprecated(forRemoval = true)
    static Context create(String name, Object... members) {
        return new Base(Base.ROOT, name, members);
    }

    /**
     * @deprecated Use {@link Context#root()} instead of creating new contexts
     */
    @Deprecated(forRemoval = true)
    static Context create(Context parent, Object... members) {
        return new Base(parent, callerClass(1), members);
    }

    @Deprecated(forRemoval = true)
    static <T> T requireFromContexts(Class<T> member) throws NoSuchElementException {
        return requireFromContexts(member, false);
    }

    @Deprecated(forRemoval = true)
    static <T> T requireFromContexts(Class<T> member, boolean includeChildren) throws NoSuchElementException {
        return requireFromContexts(member, String.format("No member of type %s found", member));
    }

    @Deprecated(forRemoval = true)
    static <T> T requireFromContexts(Class<T> member, String message) throws NoSuchElementException {
        return requireFromContexts(member, message, false);
    }

    @Deprecated(forRemoval = true)
    static <T> T requireFromContexts(Class<T> member, String message, boolean includeChildren)
    throws NoSuchElementException {
        return getFromContexts(member, includeChildren).orElseThrow(() -> new NoSuchElementException(message));
    }

    @Deprecated(forRemoval = true)
    static <T> Wrap<T> getFromContexts(final Class<T> member) {
        return getFromContexts(member, false);
    }

    @Deprecated(forRemoval = true)
    static <T> Wrap<T> getFromContexts(final Class<T> member, boolean includeChildren) {
        return () -> Base.ROOT.get()
                .getChildren()
                .stream()
                .flatMap(sub -> sub.getFromContext(member, includeChildren).stream())
                .findFirst()
                .orElse(null);
    }

    @Internal
    @Deprecated(forRemoval = true)
    static String wrapContextStr(String subStr) {
        return "Context<" + subStr + ">";
    }

    @Deprecated(forRemoval = true)
    static <T> @Nullable T getFromRoot(Class<T> type) {
        return get(type);
    }

    @Deprecated(forRemoval = true)
    static <T> T getFromRoot(Class<T> type, Supplier<? extends T> elseGet) {
        return wrap(type).orElseGet(elseGet);
    }

    default Collection<Context> getChildren() {
        return Collections.emptySet();
    }

    @Internal
    @NonExtendable
    default boolean isRoot() {
        return getParentContext() == null;
    }

    @Internal
    default @Nullable Context getParentContext() {
        return null;
    }

    @NonExtendable
    default <T> Wrap<T> getFromContext(final Class<T> type, boolean includeChildren) {
        return () -> streamContextMembers(includeChildren, type).filter(type::isInstance)
                .findFirst()
                .map(type::cast)
                .orElse(null);
    }

    default Stream<Object> streamContextMembers(boolean includeChildren) {
        return uncheckedCast(streamContextMembers(includeChildren, Object.class));
    }

    default <T> Stream<? extends T> streamContextMembers(boolean includeChildren, Class<T> type) {
        return root().streamContextMembers(includeChildren, type);
    }

    default <T> Serializer<T> findSerializer(@Nullable CharSequence mimetype) {
        return streamContextMembers(true).filter(Serializer.class::isInstance)
                .map(Serializer.class::cast)
                .filter(seri -> mimetype == null || seri.getMimeType().equals(mimetype.toString()))
                .findFirst()
                .map(Polyfill::<Serializer<T>>uncheckedCast)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        "No Serializer with Mime Type %s was found in %s",
                        mimetype,
                        this)));
    }

    @Deprecated(forRemoval = true)
    default Stream<Object> streamContextMembers() {
        return streamContextMembers(false);
    }

    @NonExtendable
    @Deprecated(forRemoval = true)
    default <T> Wrap<T> getFromContext(final Class<T> memberType) {
        return getFromContext(memberType, false);
    }

    @NonExtendable
    @Deprecated(forRemoval = true)
    default Context plus() {
        return plus(new Object[0]);
    }

    @NonExtendable
    default Context plus(Object... plus) {
        return plus(callerClass(1).getSimpleName(), plus);
    }

    @NonExtendable
    default Context plus(String name, Object... plus) {
        return new Base(this, name, plus);
    }

    @Deprecated(forRemoval = true)
    default boolean addToContext(Object... plus) {
        return false;
    }

    @NonExtendable
    @Deprecated(forRemoval = true)
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType) throws NoSuchElementException {
        return requireFromContext(memberType, false);
    }

    @NonExtendable
    @Deprecated(forRemoval = true)
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType, boolean includeChildren)
    throws NoSuchElementException {
        return requireFromContext(memberType, String.format("No member of type %s found in %s", memberType, this));
    }

    @NonExtendable
    @Deprecated(forRemoval = true)
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType, String message)
    throws NoSuchElementException {
        return requireFromContext(memberType, message, false);
    }

    @NonExtendable
    @Deprecated(forRemoval = true)
    default <T> @NotNull T requireFromContext(
            final Class<? super T> memberType, String message,
            boolean includeChildren
    ) throws NoSuchElementException {
        return uncheckedCast(getFromContext(memberType, includeChildren).assertion(String.format("<%s => %s>",
                this,
                message)));
    }

    @Deprecated(forRemoval = true)
    interface Underlying extends Context {
        default Context getUnderlyingContextualProvider() {
            return root();
        }
    }

    @Experimental
    @Deprecated(forRemoval = true)
    interface This<T> extends Context {
        @Override
        default String getName() {
            Class<? extends This> cls = getClass();
            if (cls == null) return "uninitialized context";
            return wrapContextStr(cls.getSimpleName());
        }

        default Stream<Object> streamContextMembers(boolean includeChildren) {
            return Stream.of(this);
        }

        @Override
        default Context plus(Object... plus) {
            return create(this, plus);
        }
    }

    @Experimental
    @Deprecated(forRemoval = true) // idek what this was used for lol
    interface Member<T> extends Context {
        @Override
        default String getName() {
            return wrapContextStr(getFromContext().getClass().getSimpleName());
        }

        T getFromContext();

        @Override
        default Stream<Object> streamContextMembers(boolean includeChildren) {
            return Stream.of(getFromContext());
        }

        @Override
        default Context plus(Object... plus) {
            return create(getFromContext(), plus);
        }
    }

    @Internal
    class Base implements Context {
        @SuppressWarnings("ConstantConditions") public static final Supplier<Context> ROOT;

        static {
            ROOT = Wrap.onDemand(() -> ServiceLoader.load(RootContextSource.class)
                    .findFirst()
                    .map(RootContextSource::getRootContext)
                    .orElseGet(() -> {
                        try {
                            var rootContext = new Context.Base(null, "ROOT", new Object[0]);
                            InputStream resource = ClassLoader.getSystemClassLoader()
                                    .getResourceAsStream("org/comroid/api/context.properties");
                            if (resource != null) {
                                Properties props = new Properties();
                                props.load(resource);

                                int      c      = 0;
                                Object[] values = new Object[props.size()];
                                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                                    final int fc          = c;
                                    Class<?>  targetClass = Class.forName(String.valueOf(entry.getValue()));
                                    createInstance(targetClass).ifPresent(it -> values[fc] = it);
                                    c++;
                                }
                                Debug.logger.log(Level.FINE,
                                        "Initializing ContextualProvider Root with: {}",
                                        Arrays.toString(values));
                                rootContext.addToContext(values);
                            }
                            return rootContext;
                        } catch (IOException e) {
                            throw new RuntimeException("Could not read context properties", e);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not find Context Class", e);
                        }
                    }));
        }

        @Getter protected final Set<Context> children;
        @Getter protected final Set<Object> myMembers;
        private final           Context      parent;
        private final           String       name;

        protected Base(@NotNull Context parent, Object... initialMembers) {
            this(parent, callerClass(1).getSimpleName(), initialMembers);
        }

        protected Base(String name, Object... initialMembers) {
            this(null, name, initialMembers);
        }

        protected Base(Object... initialMembers) {
            this(null, "ROOT", initialMembers);
        }

        protected Base(Context parent, String name, Object... initialMembers) {
            this.myMembers = new HashSet<>();
            this.children = new HashSet<>();
            this.parent   = name.equals("ROOT") && callerClass(1).equals(Context.Base.class)
                            ? parent
                            : Objects.requireNonNull(parent);
            this.name     = name;
            if (!isRoot()) parent.addToContext(this);
            addToContext(initialMembers);
        }

        @Override
        public final @Nullable Context getParentContext() {
            return parent;
        }

        @SneakyThrows
        public final <T> Stream<? extends T> streamContextMembers(boolean includeChildren, final Class<T> type) {
            Stream<Object> stream1 = Stream.concat(Stream.of(parent)
                    .filter(Objects::nonNull)
                    .flatMap(contextualProvider -> contextualProvider.streamContextMembers(false)), Stream.of(this));

            Stream<Object> stream2 = Stream.concat(myMembers.stream(),
                    includeChildren
                    ? children.stream().flatMap(sub -> sub.streamContextMembers(includeChildren))
                    : Stream.empty());

            // stream beans if we are in a spring environment
            // needs to call 'new org.springframework.context.support.StaticApplicationContext().getBeansOfType(type)'
            Stream<Object> stream3 = Wrap.ofSupplier(ThrowingSupplier.fallback(() -> Class.forName(
                            "org.springframework.context.support.StaticApplicationContext")))
                    .stream()
                    .map(ReflectionHelper::obtainInstance)
                    .flatMap(Wrap::stream)
                    .filter(Objects::nonNull)
                    .flatMap(ctx -> ReflectionHelper.<Map<String, Object>>call(ctx, "getBeansOfType", type)
                            .values()
                            .stream());

            return Stream.concat(Stream.concat(stream1, stream2), stream3).filter(Objects::nonNull)
                    //.flatMap(it -> {if (it.getClass().isArray()) return Stream.of((Object[]) it);return Stream.of(it);})
                    .filter(type::isInstance).map(type::cast).distinct();
        }

        @Override
        public String getName() {
            return wrapContextStr(name);
        }

        @Override
        public String toString() {
            return getName();
        }

        private static Wrap<?> createInstance(Class<?> targetClass) {
            return ReflectionHelper.obtainInstance(targetClass);
        }
    }
}
