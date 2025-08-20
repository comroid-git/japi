package org.comroid.api.func.util;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.ICommandReference;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.apache.logging.log4j.Level;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.Default;
import org.comroid.annotations.Doc;
import org.comroid.annotations.Instance;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.Aliased;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.attr.LongAttribute;
import org.comroid.api.attr.Named;
import org.comroid.api.attr.StringAttribute;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.data.seri.type.ArrayValueType;
import org.comroid.api.data.seri.type.BoundValueType;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.Specifiable;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.java.Activator;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Capitalization;
import org.comroid.api.text.StringMode;
import org.comroid.api.text.Translation;
import org.comroid.api.tree.Container;
import org.comroid.api.tree.Initializable;
import org.comroid.api.tree.UncheckedCloseable;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.function.Function.*;
import static java.util.function.Predicate.*;
import static java.util.stream.Stream.*;
import static java.util.stream.Stream.of;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.*;
import static org.comroid.api.func.util.Debug.*;
import static org.comroid.api.func.util.Streams.*;

@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Command {
    String EmptyAttribute = "@@@";

    String value() default EmptyAttribute;

    String usage() default EmptyAttribute;

    String permission() default EmptyAttribute;

    PrivacyLevel privacy() default PrivacyLevel.EPHEMERAL;

    enum PrivacyLevel {PUBLIC, PRIVATE, EPHEMERAL}

    enum Capability {
        NAMED_ARGS;

        @FunctionalInterface
        public interface Provider {
            Set<Capability> getCapabilities();

            default boolean hasCapability(Capability capability) {
                return getCapabilities().stream().anyMatch(capability::equals);
            }
        }
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String value() default EmptyAttribute;

        int index() default -1;

        String[] autoFill() default { };

        Class<? extends AutoFillProvider>[] autoFillProvider() default { };

        boolean required() default true;

        StringMode stringMode() default StringMode.NORMAL;
    }

    @FunctionalInterface
    interface ContextProvider {
        Stream<Object> expandContext(Object... context);
    }

    @FunctionalInterface
    interface PermissionChecker {
        PermissionChecker ALLOW_ALL = (usage, key) -> true;
        PermissionChecker DENY_ALL  = (usage, key) -> false;

        static PermissionChecker minecraft(Adapter adapter) {
            return (usage, key) -> {
                var userId = usage.getContext().stream().flatMap(cast(UUID.class)).findAny().orElseThrow();
                return key instanceof Integer level
                       ? adapter.checkOpLevel(userId, level)
                       : adapter.checkPermission(userId, key.toString(), false).toBooleanOrElse(false);
            };
        }

        static Error insufficientPermissions(@Nullable String detail) {
            return new Error("You dont have permission to do this " + (detail == null ? "\b" : detail));
        }

        default boolean userHasPermission(Usage usage) {
            return getPermissionKey(usage).filter(key -> userHasPermission(usage, key)).isPresent();
        }

        default Optional<Object> getPermissionKey(Usage usage) {
            return Optional.of(usage.getStackTrace().peek().getAttribute().permission())
                    .filter(Predicate.<String>not(Command.EmptyAttribute::equals).and(not(String::isBlank)))
                    .map(StandardValueType::findGoodType);
        }

        boolean userHasPermission(Usage usage, Object key);

        interface Adapter {
            default boolean checkOpLevel(UUID playerId) {
                return checkOpLevel(playerId, 1);
            }

            boolean checkOpLevel(UUID playerId, @MagicConstant(intValues = { 1, 2, 3, 4 }) int minimum);

            default TriState checkPermission(UUID playerId, String key) {return checkPermission(playerId, key, false);}

            TriState checkPermission(UUID playerId, String key, boolean explicit);
        }
    }

    @FunctionalInterface
    interface AutoFillProvider {
        Stream<String> autoFill(Usage usage, String argName, String currentValue);

        default Predicate<String> stringCheck(String currentValue) {
            return str -> {
                if (currentValue.isBlank() || currentValue.endsWith(" ")) return true;
                return currentValue.contains("*")
                       // wildcard mode
                       ? str.toLowerCase().matches(currentValue.toLowerCase().replace("*", "(\\*|.*?)"))
                       // normal filter
                       : str.toLowerCase().startsWith(currentValue.toLowerCase());
            };
        }

        enum Duration implements Strings {
            @Instance INSTANCE;

            private static final String[] suffixes = new String[]{ "min", "h", "d", "w", "Mon", "y" };
            private static final int[]    charMask = Arrays.stream(suffixes)
                    .flatMapToInt(String::chars)
                    .distinct()
                    .toArray();
            private static final int[]    longMask = Arrays.stream(suffixes)
                    .flatMapToInt(str -> str.chars().skip(1))
                    .distinct()
                    .toArray();

            @Override
            public Stream<String> strings(Usage usage, String currentValue) {
                if (currentValue.isEmpty())
                    // example values
                    return of("5m", "6h", "3d", "2w", "6Mon", "1y");

                // find last char
                var chars = currentValue.toCharArray();
                var lc = chars[chars.length - 1];

                var offset = 0;
                if (Arrays.binarySearch(charMask, lc) != -1) offset += 1;
                if (Arrays.binarySearch(longMask, lc) != -1) offset += 1;
                if (lc == 'n') offset += 1;

                final var cutoff = offset;
                return Arrays.stream(suffixes)
                        .filter(str -> str.length() - 1 >= cutoff)
                        .map(str -> str.substring(cutoff))
                        .filter(not(String::isBlank))
                        .map(str -> currentValue + str);
            }
        }

        interface Strings extends AutoFillProvider {
            @Override
            default Stream<String> autoFill(Usage usage, String argName, String currentValue) {
                return strings(usage, currentValue).filter(stringCheck(currentValue));
            }

            Stream<String> strings(Usage usage, String currentValue);
        }

        interface Adapter<T> extends Strings {
            @Override
            default Stream<String> strings(Usage usage, String currentValue) {
                return objects(usage, currentValue).map(this::toString);
            }

            String toString(T object);

            Stream<T> objects(Usage usage, String currentValue);
        }

        interface Named<T extends org.comroid.api.attr.Named> extends Adapter<T> {
            @Override
            default String toString(T object) {
                return object.getPrimaryName();
            }
        }

        @Value
        class Array implements Strings {
            String[] options;

            public Array(String... options) {
                this.options = options;
            }

            @Override
            public Stream<String> strings(Usage usage, String currentValue) {
                return of(options);
            }
        }

        @Value
        class Enum<T extends java.lang.Enum<? super T>> implements AutoFillProvider.Adapter<T> {
            Class<T> type;

            @Override
            public String toString(T object) {
                return object.name();
            }

            @Override
            public Stream<T> objects(Usage usage, String currentValue) {
                return Arrays.stream(type.getEnumConstants());
            }
        }
    }

    @FunctionalInterface
    interface Handler {
        @Deprecated
        default Capitalization getDesiredKeyCapitalization() {
            return Capitalization.IDENTITY;
        }

        void handleResponse(Usage command, @NotNull Object response, Object... args);

        default String handleThrowable(Throwable throwable) {
            while (throwable instanceof InvocationTargetException itex) throwable = throwable.getCause();
            var msg = "%s: %s".formatted(throwable instanceof Error
                                         ? throwable.getClass().getSimpleName()
                                         : StackTraceUtils.lessSimpleName(throwable.getClass()),
                    throwable.getMessage() == null ? throwable.getCause() == null
                                                     ? "Internal Error"
                                                     : throwable.getCause() instanceof Error
                                                       ? throwable.getCause()
                                                               .getMessage()
                                                       : throwable.getCause()
                                                                 .getClass()
                                                                 .getSimpleName() + ": " + throwable.getCause()
                                                                 .getMessage() : throwable.getMessage());
            if (throwable instanceof Error) return msg;
            var buf = new StringWriter();
            var out = new PrintStream(new DelegateStream.Output(buf));
            out.println(msg);
            Throwable cause = throwable;
            do {
                var c = cause.getCause();
                if (c == null) break;
                cause = c;
            } while (cause instanceof InvocationTargetException || (cause instanceof RuntimeException && cause.getCause() instanceof InvocationTargetException));
            StackTraceUtils.wrap(cause, out, true);
            var str = buf.toString();
            if (str.length() > 1950) str = str.substring(0, 1950);
            return str;
        }

        interface Minecraft extends Handler {
            @Override
            default String handleThrowable(Throwable throwable) {
                return "§c" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            }
        }
    }

    interface Info extends Capability.Provider, ContextProvider, Initializable {}

    record AutoFillOption(String key, String description) {}

    @Value
    @Builder
    class Usage {
        Manager  manager;
        String[] fullCommand;
        @NotNull                       Handler                     source;
        @NotNull                       Node.Callable               baseNode;
        @Default                       Stack<Node.Callable>        stackTrace      = new Stack<>();
        @Default                       Stack<Node.Parameter>       paramTrace      = new Stack<>();
        @Default @Singular("argument") Map<Node.Parameter, String> argumentStrings = new ConcurrentHashMap<>();
        @Default @Singular("context")  Set<Object>                 context;

        public void advanceFull() {
            stackTrace.clear();
            paramTrace.clear();

            stackTrace.push(baseNode);

            var helper = new Object() {
                Node          node             = baseNode;
                Node.Callable lastCallable     = null;
                int           fullCommandIndex = 1;

                boolean findNext() {
                    if (fullCommandIndex >= fullCommand.length) return false;
                    var part = fullCommand[fullCommandIndex];

                    callable:
                    if (node instanceof Node.Callable callable) {
                        var result = callable.nodes().filter(node -> Objects.equals(part, node.getName())).findAny();
                        if (result.isEmpty()) break callable;
                        this.node         = result.get();
                        this.lastCallable = result.flatMap(Optionals.cast(Node.Callable.class)).orElse(callable);
                        this.fullCommandIndex += 1;
                        return true;
                    }

                    if (lastCallable == null) return false;

                    var params = lastCallable.nodes().flatMap(Streams.cast(Node.Parameter.class)).toList();
                    node = params.getFirst();
                    if (node instanceof Node.Parameter param) {
                        // store argString
                        var argString = new StringBuilder(fullCommand[fullCommandIndex]);
                        switch (param.getAttribute().stringMode()) {
                            case NORMAL, GREEDY -> {
                                if (!argString.toString().startsWith("\"")) break;
                                // immediately consume greedy argument
                                argString = new StringBuilder(argString.substring(1));
                                String next;
                                do {
                                    argString.append(next = fullCommand[++fullCommandIndex]);
                                } while (!next.endsWith("\"") && fullCommandIndex + 1 < fullCommand.length);
                                if (next.endsWith("\"")) argString.deleteCharAt(argString.length() - 1);
                            }
                        }
                        argumentStrings.put(param, argString.toString());

                        // advance parameter if possible
                        var nextIndex = params.indexOf(param) + 1;
                        if (nextIndex == -1) return false;
                        if (nextIndex >= params.size() && param.getAttribute().stringMode() == StringMode.SINGLE_WORD)
                            return false;
                        this.node = params.get(nextIndex + 1);
                        this.fullCommandIndex += 1;
                        return true;
                    }

                    return false;
                }

                void commit() {
                    if (node instanceof Node.Callable callable) stackTrace.push(callable);
                    if (node instanceof Node.Parameter parameter) paramTrace.push(parameter);
                }
            };

            while (helper.findNext()) helper.commit();
        }
    }

    @Value
    @Log4j2
    @NonFinal
    @ToString(of = { "id" })
    class Manager extends Container.Base implements Info, PermissionChecker {
        public static final Handler DefaultHandler = (command, x, args) -> System.out.println(x);
        UUID      id        = UUID.randomUUID();
        Set<Node> baseNodes = new HashSet<>();

        @Override
        public final Set<Capability> getCapabilities() {
            return streamChildren(Capability.Provider.class).flatMap(provider -> provider.getCapabilities().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @SuppressWarnings("UnusedReturnValue")
        public final Set<Node> register(final Object target) {
            var klass  = target instanceof Class<?> cls0 ? cls0 : target.getClass();
            var groups = new ArrayList<Node.Group>();
            var calls  = new ArrayList<Node.Call>();

            registerGroups(target, groups, klass);
            registerCalls(target, calls, klass);

            Set<Node> nodes;
            var       attr = klass.getAnnotation(Command.class);
            if (attr != null) nodes = Set.of(Node.Group.builder()
                    .attribute(attr)
                    .name(EmptyAttribute.equals(attr.value()) ? klass.getSimpleName() : attr.value())
                    .source(klass)
                    .groups(groups)
                    .calls(calls)
                    .defaultCall(calls.stream().filter(call -> "$".equals(call.name())).findAny().orElse(null))
                    .build());
            else nodes = Stream.concat(groups.stream(), calls.stream()).collect(Collectors.toUnmodifiableSet());

            baseNodes.addAll(nodes);
            return nodes;
        }

        public final Node.Group createGroupNode(@Nullable Object target, Class<?> source) {
            var attribute = Annotations.findAnnotations(Command.class, source)
                    .findFirst()
                    .orElseThrow()
                    .getAnnotation();
            var group = Node.Group.builder()
                    .name(EmptyAttribute.equals(attribute.value()) ? source.getSimpleName() : attribute.value())
                    .attribute(attribute)
                    .source(source)
                    .attribute(attribute);

            var groups = new HashSet<Node.Group>();
            registerGroups(target, groups, source);
            group.groups(groups);

            var calls = new HashSet<Node.Call>();
            registerCalls(target, calls, source);
            var defaultCall = calls.stream().filter(call -> "$".equals(call.getName())).findAny().orElse(null);
            if (defaultCall != null) calls.remove(defaultCall);
            group.calls(calls).defaultCall(defaultCall);

            return group.build();
        }

        public final Node.Call createCallNode(@Nullable Object target, Method source) {
            var attribute = Annotations.findAnnotations(Command.class, source)
                    .findFirst()
                    .orElseThrow()
                    .getAnnotation();
            var call = Node.Call.builder()
                    .name(EmptyAttribute.equals(attribute.value()) ? source.getName() : attribute.value())
                    .attribute(attribute)
                    .target(target)
                    .method(source)
                    .callable(Invocable.ofMethodCall(target, source));

            var params = new ArrayList<Node.Parameter>();
            registerParameters(params, source);
            params.sort(Node.Parameter.COMPARATOR);
            call.parameters(unmodifiableList(params));

            return call.build();
        }

        @Override
        public final void initialize() {
            streamChildren(Adapter.class).forEach(Adapter::initialize);
        }

        public final Stream<AutoFillOption> autoComplete(
                Handler source,
                @Doc("Do not include currentValue") String[] fullCommand,
                String argName, @Nullable String currentValue
        ) {
            var usage = createUsageBase(source, fullCommand);
            return autoComplete(usage, argName, currentValue);
        }

        protected final Usage createUsageBase(Handler source, String[] fullCommand, Object... baseArgs) {
            var baseNode = baseNodes.stream() // find base node to initiate advancing to execution node
                    .filter(node -> node.names().anyMatch(fullCommand[0]::equals))
                    .flatMap(cast(Node.Callable.class))
                    .findAny()
                    .orElseThrow(() -> new Error("No such command: " + Arrays.toString(fullCommand)));
            return Usage.builder()
                    .source(source)
                    .manager(this)
                    .fullCommand(trimFullCommand(fullCommand))
                    .context(expandContext(concat(of(this, source), Arrays.stream(baseArgs)).toArray()).collect(
                            Collectors.toSet()))
                    .baseNode(baseNode)
                    .build();
        }

        private String[] trimFullCommand(String[] fullCommand) {
            var ls = new ArrayList<String>();
            for (var i = 0; i < fullCommand.length; i++) {
                var part = fullCommand[i];
                if (part.isBlank() && ls.getLast().isBlank()) break;
                ls.add(part);
            }
            return ls.toArray(String[]::new);
        }

        @Override
        public final Stream<Object> expandContext(Object... baseContext) {
            return streamChildren(ContextProvider.class).flatMap(multiply(provider -> provider.expandContext(baseContext)));
        }

        public final Stream<AutoFillOption> autoComplete(Usage usage, String argName, @Nullable String currentValue) {
            try {
                usage.advanceFull();
                //todo verifyPermission(usage);
                var paramTrace = usage.getParamTrace();
                return (paramTrace.isEmpty()
                        ? usage.getStackTrace().peek().nodes().map(Node::getName)
                        : paramTrace.peek().autoFill(usage, argName, currentValue)).map(str -> new AutoFillOption(str,
                        str));
            } catch (Throwable e) {
                log.log(isDebug() ? Level.WARN : Level.DEBUG, "An error ocurred during command autocompletion", e);
                return of(usage.source.handleThrowable(e)).map(String::valueOf).map(str -> new AutoFillOption(str, ""));
            }
        }

        private void verifyPermission(Usage usage) {
            var opt = getPermissionKey(usage);
            if (opt.isEmpty()) return;
            var key = opt.get();
            if (!userHasPermission(usage, key))
                throw PermissionChecker.insufficientPermissions("(missing permission: '%s')".formatted(key));
        }

        @Override
        public final boolean userHasPermission(Usage usage, Object key) {
            return streamChildren(PermissionChecker.class).anyMatch(pc -> pc.userHasPermission(usage, key));
        }

        public final @Nullable Object execute(
                Handler source, String[] fullCommand, @Nullable Map<String, Object> namedArgs, Object... extraArgs) {
            var usage = createUsageBase(source, fullCommand, extraArgs);
            return execute(usage, namedArgs);
        }

        public final @Nullable Object execute(Usage usage, @Nullable Map<String, Object> namedArgs) {
            Object result = null, response;
            try {
                usage.advanceFull();
                verifyPermission(usage);

                Node.Call call = usage.stackTrace.peek().asCall();
                if (call == null) throw new Error("No such command");

                // sort arguments
                var parameters = call.getParameters();
                var paramIndex = new int[]{ 0 };

                // decide arg handling type
                var argStringSource = (hasCapability(Capability.NAMED_ARGS)
                                       ? (Function<String, Stream<Node.Parameter>>) key -> parameters.stream()
                        .filter(p -> p.getName().equals(key))
                                       : (Function<String, Stream<Node.Parameter>>) $ -> parameters.stream()
                                               .sorted(Comparator.comparingInt(Node.Parameter::getIndex))
                                               .skip(paramIndex[0])).andThen(src -> src.map(usage.getArgumentStrings()::get));

                // parse args
                var useArgs = parameters.stream().map(param -> {
                    var vt = ValueType.of(param.getParam().getType());
                    return argStringSource.apply(param.getName())
                            .findAny()
                            .map(vt::parse)
                            .or(() -> usage.getContext().stream().filter(vt.getTargetClass()::isInstance).findAny())
                            .orElse(null);
                }).toArray();

                // execute method
                result = response = call.callable.invoke(call.target, useArgs);
            } catch (Error err) {
                response = err.response == null ? usage.source.handleThrowable(err) : err.response;
            } catch (Throwable e) {
                log.log(isDebug() ? Level.ERROR : Level.DEBUG, "An error ocurred during command execution", e);
                response = usage.source.handleThrowable(e);
            }
            if (response != null) usage.source.handleResponse(usage, response, usage.context.toArray());
            return result;
        }

        protected Optional<Adapter> adapter() {
            return streamChildren(Adapter.class).findAny();
        }

        private void registerGroups(@Nullable Object target, Collection<? super Node.Group> nodes, Class<?> source) {
            for (var groupNodeSource : source.getClasses()) {
                if (!groupNodeSource.isAnnotationPresent(Command.class)) continue;
                var node = createGroupNode(target, groupNodeSource);
                nodes.add(node);
            }
        }

        private Node.Parameter createParameterNode(int index, Method origin, Parameter source) {
            var attribute = Annotations.findAnnotations(Arg.class, source).findFirst().orElseThrow().getAnnotation();
            // construct parameter node
            var builder = Node.Parameter.builder()
                    .name(Optional.ofNullable(attribute.value())
                            .filter(not(EmptyAttribute::equals))
                            .or(() -> Optional.ofNullable(source.getName()).filter(name -> !name.matches("arg\\d+")))
                            .or(() -> Aliased.$(source).findFirst())
                            .orElse(String.valueOf(index)))
                    .attribute(attribute)
                    .param(source)
                    .required(attribute.required())
                    .index(index);

            // init special types
            if (source.getType().isEnum()) builder.autoFillProvider(new AutoFillProvider.Enum(Polyfill.uncheckedCast(
                    source.getType())));
            else if (attribute.autoFill().length > 0) builder.autoFillProvider(new AutoFillProvider.Array(attribute.autoFill()));

            // init custom autofill providers
            for (var providerType : attribute.autoFillProvider()) {
                var provider = ReflectionHelper.instanceField(providerType)
                        .stream()
                        .flatMap(cast(AutoFillProvider.class))
                        .findAny()
                        .orElseGet(() -> Activator.get(providerType).createInstance(JSON.Parser.createObjectNode()));
                builder.autoFillProvider(provider);
            }
            return builder.build();
        }

        private void registerCalls(@Nullable Object target, Collection<? super Node.Call> nodes, Class<?> source) {
            for (var callNodeSource : source.getMethods()) {
                if (!callNodeSource.isAnnotationPresent(Command.class)) continue;
                var node = createCallNode(target, callNodeSource);
                nodes.add(node);
            }
        }

        private void registerParameters(Collection<? super Node.Parameter> nodes, Method source) {
            var index = 0;
            for (var paramNodeSource : source.getParameters()) {
                if (!paramNodeSource.isAnnotationPresent(Arg.class)) continue;
                var node = createParameterNode(index, source, paramNodeSource);
                nodes.add(node);
                index += 1;
            }
        }

        @Override
        public final int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Manager && obj.hashCode() == hashCode();
        }

        @Value
        public class Adapter$StdIO extends Adapter {
            InputStream in;
            PrintStream out;

            public Adapter$StdIO() {
                this(System.in, System.out);
            }

            public Adapter$StdIO(InputStream in, OutputStream out) {
                this.in  = in;
                this.out = out instanceof PrintStream ps ? ps : new PrintStream(out);
            }

            @Override
            public Set<Capability> getCapabilities() {
                return Set.of();
            }

            @Override
            public void initialize() {
                CompletableFuture.supplyAsync(this::inputReader, Executors.newSingleThreadExecutor())
                        .exceptionally(exceptionLogger("A fatal error occurred in the Input reader"));
            }

            @Override
            public void handleResponse(Usage command, @NotNull Object response, Object... args) {
                out.println(response);
            }

            @SneakyThrows
            private Void inputReader() {
                try (
                        var isr = new InputStreamReader(in); var br = new BufferedReader(isr)
                ) {
                    String line;
                    do {
                        line = br.readLine();
                        if ("exit".equals(line)) break;
                        Manager.this.execute(this, line.split(" "), Map.of(), Manager.this);
                    } while (true);
                    System.exit(0);
                }
                return null;
            }
        }

        @Value
        @RequiredArgsConstructor
        public class Adapter$JDA extends Adapter implements PermissionChecker {
            Set<Capability>         capabilities = Set.of(Capability.NAMED_ARGS);
            JDA                     jda;
            Event.Bus<GenericEvent> bus          = new Event.Bus<>();
            @Nullable @NonFinal @Setter BiFunction<EmbedBuilder, User, EmbedBuilder> embedFinalizer = null;
            @Setter @NonFinal           boolean                                      initialized    = false;
            @Setter @NonFinal
            boolean purgeCommands = false;//Debug.isDebug();

            {
                addChild(this);
            }

            @Override
            public void initialize() {
                if (initialized) return;

                jda.addEventListener(new ListenerAdapter() {
                    @Override
                    public void onGenericEvent(@NotNull GenericEvent event) {
                        bus.publish(event);
                    }
                });
                bus.flatMap(SlashCommandInteractionEvent.class)
                        .listen()
                        .subscribeData(event -> execute(Adapter$JDA.this,
                                event.getCommandString().substring(1)/*.replaceAll("(\\w+):","$1")*/.split(" "),
                                event.getOptions()
                                        .stream()
                                        .collect(Collectors.toMap(OptionMapping::getName,
                                                mapping -> switch (mapping.getType()) {
                                                    case STRING -> mapping.getAsString();
                                                    case INTEGER -> mapping.getAsInt();
                                                    case BOOLEAN -> mapping.getAsBoolean();
                                                    case USER -> mapping.getAsUser();
                                                    case CHANNEL -> mapping.getAsChannel();
                                                    case ROLE -> mapping.getAsRole();
                                                    case MENTIONABLE -> mapping.getAsMentionable();
                                                    case NUMBER -> mapping.getAsDouble();
                                                    case ATTACHMENT -> mapping.getAsAttachment();
                                                    default ->
                                                            throw new IllegalStateException("Unexpected value: " + mapping.getType());
                                                })),
                                event.getName(),
                                event,
                                event.getUser(),
                                event.getMember(),
                                event.getGuild(),
                                event.getChannel()));
                bus.flatMap(CommandAutoCompleteInteractionEvent.class).listen().subscribeData(event -> {
                    var option = event.getFocusedOption();
                    var options = autoComplete(Adapter$JDA.this,
                            event.getCommandString().substring(1).split(" "),
                            option.getName(),
                            option.getValue()).map(e -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(e.key,
                            e.description)).limit(25).toList();
                    event.replyChoices(options).queue();
                });

                var helper = new Object() {
                    public Stream<Node.Call> expandToCallNodes(Node node) {
                        return of(node).flatMap(it -> {
                            if (it instanceof Node.Group group) return group.nodes().flatMap(this::expandToCallNodes);
                            return of(it).flatMap(cast(Node.Call.class));
                        });
                    }
                };

                registerCommands();

                initialized = true;
            }

            private void registerCommands() {
                var cmds = new ArrayList<SlashCommandData>();
                jda.retrieveCommands().flatMap(existing -> {
                    RestAction<?> chain = null;
                    if (purgeCommands) for (var ex : existing)
                        chain = chain == null
                                ? jda.deleteCommandById(ex.getId())
                                : chain.flatMap($ -> jda.deleteCommandById(ex.getId()));

                    for (var node : baseNodes) {
                        if (!purgeCommands && existing.stream()
                                .map(ICommandReference::getName)
                                .anyMatch(node.name::equalsIgnoreCase)) continue;

                        SlashCommandData cmd = Commands.slash(node.name.toLowerCase(), node.getDescription());

                        switch (node) {
                            case Node.Group group -> {
                                for (var callable : group.nodes().toList()) {
                                    if (callable instanceof Node.Group g0) cmd.addSubcommandGroups(makeGroup(g0));
                                    if (callable instanceof Node.Call c0) cmd.addSubcommands(makeMember(c0));
                                }
                            }
                            case Node.Call call -> {
                                var perm = call.getAttribute().permission();
                                if (perm.matches("\\d+")) cmd.setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                                        Long.parseLong(perm)));
                                for (var parameter : call.parameters) {
                                    cmd.addOption(optionType(parameter),
                                            parameter.name().toLowerCase(),
                                            parameter.getDescription(),
                                            parameter.required,
                                            !parameter.autoFillProviders.isEmpty());
                                }
                            }
                            default -> {}
                        }

                        chain = chain == null ? jda.upsertCommand(cmd) : chain.flatMap($ -> jda.upsertCommand(cmd));
                    }

                    if (chain == null) chain = jda.retrieveApplicationInfo();
                    return chain;
                }).queue();
            }

            private SubcommandGroupData makeGroup(Node.Group group) {
                var data = new SubcommandGroupData(group.name(), group.getDescription());
                for (var sub : group.nodes().toList())
                    if (sub instanceof Node.Call call) {
                        var child = makeMember(call);
                        data.addSubcommands(child);
                    }
                return data;
            }

            private SubcommandData makeMember(Node.Call call) {
                var data = new SubcommandData(call.name(), call.getDescription());
                for (var parameter : call.parameters) {
                    data.addOption(optionType(parameter),
                            parameter.name().toLowerCase(),
                            parameter.getDescription(),
                            parameter.required,
                            !parameter.autoFillProviders.isEmpty());
                }
                return data;
            }

            private OptionType optionType(Node.Parameter parameter) {
                return Optional.of(parameter.param.getType()).flatMap(t -> {
                    if (Boolean.class.isAssignableFrom(t)) return Optional.of(OptionType.BOOLEAN);
                    if (Integer.class.isAssignableFrom(t) || Long.class.isAssignableFrom(t))
                        return Optional.of(OptionType.INTEGER);
                    if (Number.class.isAssignableFrom(t)) return Optional.of(OptionType.NUMBER);
                    if (User.class.isAssignableFrom(t) || Member.class.isAssignableFrom(t))
                        return Optional.of(OptionType.USER);
                    if (Channel.class.isAssignableFrom(t)) return Optional.of(OptionType.CHANNEL);
                    if (Role.class.isAssignableFrom(t)) return Optional.of(OptionType.ROLE);
                    if (IMentionable.class.isAssignableFrom(t)) return Optional.of(OptionType.MENTIONABLE);
                    if (Message.Attachment.class.isAssignableFrom(t)) return Optional.of(OptionType.ATTACHMENT);
                    return Optional.empty();
                }).orElse(OptionType.STRING);
            }

            private CompletableFuture<?> handleResponse(MessageSender hook, User user, Object response) {
                return (switch (response) {
                    case MessageCreateData message -> hook.send(message);
                    case EmbedBuilder embed -> {
                        if (embedFinalizer != null) embed = embedFinalizer.apply(embed, user);
                        yield hook.send(embed.build());
                    }
                    default -> hook.send(String.valueOf(response));
                });
            }

            @Override
            public void handleResponse(Usage cmd, @NotNull Object response, Object... args) {
                final var e = of(args).flatMap(cast(SlashCommandInteractionEvent.class))
                        .findAny()
                        .orElseThrow();
                final var user      = of(args).flatMap(cast(User.class)).findAny().orElseThrow();
                var ephemeral = cmd.getStackTrace().peek().attribute.privacy() != PrivacyLevel.PUBLIC;
                if (response instanceof CompletableFuture) e.deferReply()
                        .setEphemeral(ephemeral)
                        .submit()
                        .thenCombine(((CompletableFuture<?>) response),
                                (hook, resp) -> handleResponse(msg -> hook.sendMessage(msg).submit(), user, resp))
                        .thenCompose(identity())
                        .exceptionally(Polyfill.exceptionLogger());
                else {
                    ReplyCallbackAction req;
                    handleResponse(msg -> e.reply(msg).setEphemeral(ephemeral).submit(), user, response);
                }
            }

            @Override
            public boolean userHasPermission(Usage usage, Object key) {
                if (key == null || String.valueOf(key).isBlank()) return true;
                var permissions = Permission.getPermissions(Long.parseLong(key.toString()));
                return usage.context.stream()
                        .flatMap(cast(Member.class))
                        .anyMatch(usr -> usr.getIdLong() == 141476933849448448L /* kaleidox is superadmin for testing purposes */ || usr.hasPermission(
                                permissions));
            }

            @Getter
            @RequiredArgsConstructor
            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            public enum OptionAdapter implements IOptionAdapter, Named {
                Boolean(StandardValueType.BOOLEAN, OptionType.BOOLEAN) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsBoolean();
                    }
                }, Int(StandardValueType.INTEGER, OptionType.INTEGER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsInt();
                    }

                    @Override
                    protected <T> Wrap<T> attribute(Object obj) {
                        return Wrap.of(obj)
                                .filter(IntegerAttribute.class::isInstance)
                                .map(IntegerAttribute.class::cast)
                                .map(IntegerAttribute::getValue)
                                .map(Polyfill::uncheckedCast);
                    }
                }, Long(StandardValueType.LONG, OptionType.INTEGER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsLong();
                    }

                    @Override
                    protected <T> Wrap<T> attribute(Object obj) {
                        return Wrap.of(obj)
                                .filter(LongAttribute.class::isInstance)
                                .map(LongAttribute.class::cast)
                                .map(LongAttribute::getValue)
                                .map(Polyfill::uncheckedCast);
                    }
                }, Double(StandardValueType.DOUBLE, OptionType.NUMBER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsDouble();
                    }
                }, String(StandardValueType.STRING, OptionType.STRING) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsString();
                    }

                    @Override
                    protected <T> Wrap<T> attribute(Object obj) {
                        return Wrap.of(obj)
                                .filter(StringAttribute.class::isInstance)
                                .map(StringAttribute.class::cast)
                                .map(StringAttribute::getString)
                                .or(() -> Named.$(obj))
                                .map(Polyfill::uncheckedCast);
                    }
                }, Attachment(BoundValueType.of(Message.Attachment.class), OptionType.ATTACHMENT) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsAttachment();
                    }
                }, Mentionable(BoundValueType.of(IMentionable.class), OptionType.MENTIONABLE) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsMentionable();
                    }
                }, Channel(BoundValueType.of(GuildChannelUnion.class), OptionType.CHANNEL) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsChannel();
                    }
                }, Role(BoundValueType.of(Role.class), OptionType.ROLE) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsRole();
                    }
                }, User(BoundValueType.of(User.class), OptionType.USER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsUser();
                    }
                }, Member(BoundValueType.of(Member.class), OptionType.USER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsMember();
                    }
                };

                public static Wrap<IOptionAdapter> of(final Class<?> type) {
                    return Wrap.of(Arrays.stream(values())
                            .filter(adp -> adp.valueType.getTargetClass().isAssignableFrom(type))
                            .findAny());
                }

                ValueType<?> valueType;
                OptionType   optionType;

                protected <T> Wrap<T> attribute(Object obj) {
                    return Wrap.empty();
                }

                @Value
                public class Enum<T> implements IOptionAdapter {
                    Class<T> enumType;

                    @Override
                    public ValueType<?> getValueType() {
                        return ArrayValueType.of(enumType);
                    }

                    @Override
                    public OptionType getOptionType() {
                        return optionType;
                    }

                    @Override
                    public Object getFrom(OptionMapping option) {
                        final var value = OptionAdapter.this.getFrom(option);
                        return Arrays.stream(enumType.getEnumConstants())
                                .filter(it -> attribute(it).contentEquals(value))
                                .findAny()
                                .orElseThrow(() -> new NoSuchElementException("Invalid enum value: " + value));
                    }
                }
            }

            public interface IOptionAdapter {

                ValueType<?> getValueType();

                OptionType getOptionType();

                Object getFrom(OptionMapping option);
            }

            @FunctionalInterface
            private interface MessageSender {
                CompletableFuture<?> send(MessageCreateData message);

                default CompletableFuture<?> send(MessageEmbed embed) {
                    return send(new MessageCreateBuilder().addEmbeds(embed).build());
                }

                default CompletableFuture<?> send(String content) {
                    return send(new MessageCreateBuilder().setContent(content).build());
                }
            }

            @Value
            @NonFinal
            public static class PaginatedList<T> extends ListenerAdapter implements UncheckedCloseable {
                public static final String   EMOJI_DELETE     = "❌";
                public static final String   EMOJI_REFRESH    = "🔄";
                public static final String   EMOJI_NEXT_PAGE  = "➡️";
                public static final String   EMOJI_PREV_PAGE  = "⬅️";
                public static final String   EMOJI_FIRST_PAGE = "⏪";
                public static final String   EMOJI_LAST_PAGE  = "⏩";
                public static final String[] EMOJI_NUMBER     = new String[]{
                        "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"
                };
                MessageChannelUnion             channel;
                Supplier<Stream<T>>             source;
                Comparator<T>                   comparator;
                Function<T, MessageEmbed.Field> toField;
                String                          title;
                int                             perPage;
                @NonFinal                   int                    page = 1;
                @NonFinal @Nullable         Message                message;
                @NonFinal @Setter @Nullable Consumer<EmbedBuilder> embedFinalizer;

                public PaginatedList(
                        MessageChannelUnion channel, Supplier<Stream<T>> source, Comparator<T> comparator,
                        Function<T, MessageEmbed.Field> toField, String title, int perPage
                ) {
                    this.channel    = channel;
                    this.source     = source;
                    this.comparator = comparator;
                    this.toField    = toField;
                    this.title      = title;
                    this.perPage    = perPage;

                    channel.getJDA().addEventListener(this);
                }

                @Override
                public void onShutdown(ShutdownEvent event) {
                    close();
                }

                @Override
                public void onMessageDelete(@NotNull MessageDeleteEvent event) {
                    if (message == null || event.getMessageIdLong() != message.getIdLong()) return;
                    message = null;
                }

                @Override
                public void onMessageBulkDelete(@NotNull MessageBulkDeleteEvent event) {
                    if (message == null || !event.getMessageIds().contains(message.getId())) return;
                    message = null;
                }

                @Override
                public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                    if (message == null || event.getMessageIdLong() != message.getIdLong() || event.getUser().isBot())
                        return;

                    try {
                        var str = event.getEmoji().getFormatted();
                        if (EMOJI_DELETE.equals(str)) {
                            close();
                            return;
                        } else if (EMOJI_REFRESH.equals(str)) {
                            refresh().queue();
                            return;
                        }

                        page = switch (str) {
                            case EMOJI_FIRST_PAGE -> 1;
                            case EMOJI_PREV_PAGE -> page - 1;
                            case EMOJI_NEXT_PAGE -> page + 1;
                            case EMOJI_LAST_PAGE -> pageCount();
                            default -> {
                                var search = Arrays.binarySearch(EMOJI_NUMBER, str);
                                yield search < 0 ? page : search;
                            }
                        };

                        refresh().queue();
                    } finally {
                        if (event.getUser() != null) event.getReaction().removeReaction(event.getUser()).queue();
                    }
                }

                @Override
                public void close() {
                    channel.getJDA().removeEventListener(this);
                    if (message != null) message.delete().queue();
                }

                public int pageCount() {
                    return (int) Math.ceil((double) source.get().count() / perPage);
                }

                public RestAction<List<Void>> resend() {
                    if (message != null) message.delete().queue();
                    return message(new MessageCreateBuilder(), msg -> channel.sendMessage(msg.build()));
                }

                public RestAction<List<Void>> refresh() {
                    if (message == null) return resend();
                    return message(new MessageEditBuilder(), msg -> message.editMessage(msg.build()));
                }

                protected void finalizeEmbed(EmbedBuilder builder) {}

                protected String pageText() {
                    return "Page %d / %d".formatted(page, pageCount());
                }

                private <R extends MessageRequest<R>> RestAction<List<Void>> message(
                        R request, Function<R, RestAction<Message>> executor) {
                    request.setEmbeds(createEmbed().build());
                    var message = executor.apply(request);
                    return refreshReactions(message);
                }

                private EmbedBuilder createEmbed() {
                    var embedBuilder = new EmbedBuilder().setTitle(title).setFooter(pageText());

                    var entries = source.get()
                            .sorted(comparator)
                            .skip((long) perPage * (page - 1))
                            .limit(perPage)
                            .map(toField)
                            .toList();
                    embedBuilder.getFields().addAll(entries);
                    finalizeEmbed(embedBuilder);

                    return embedBuilder;
                }

                private RestAction<List<Void>> refreshReactions(RestAction<Message> message) {
                    var pageCount = pageCount();
                    return message.flatMap(msg -> {
                        var emojis = concat(of(EMOJI_DELETE, EMOJI_REFRESH),
                                (pageCount <= 9
                                 ? Arrays.stream(EMOJI_NUMBER).skip(1).limit(pageCount)
                                 : of(EMOJI_FIRST_PAGE,
                                         EMOJI_PREV_PAGE,
                                         EMOJI_NEXT_PAGE,
                                         EMOJI_LAST_PAGE))).map(Emoji::fromUnicode).toList();
                        return concat(
                                // remove excess page numbers
                                Arrays.stream(EMOJI_NUMBER)
                                        .skip(1 + pageCount())
                                        .map(Emoji::fromUnicode)
                                        .filter(emoji -> msg.getReaction(emoji) != null),
                                // add new reactions
                                emojis.stream().filter(emoji -> msg.getReaction(emoji) == null)).findAny().isPresent();
                    }, msg -> {
                        this.message = msg;
                        var emojis = concat(of(EMOJI_DELETE, EMOJI_REFRESH),
                                (pageCount <= 9
                                 ? Arrays.stream(EMOJI_NUMBER).skip(1).limit(pageCount)
                                 : of(EMOJI_FIRST_PAGE,
                                         EMOJI_PREV_PAGE,
                                         EMOJI_NEXT_PAGE,
                                         EMOJI_LAST_PAGE))).map(Emoji::fromUnicode).toList();
                        return RestAction.allOf(concat(
                                // remove excess page numbers
                                Arrays.stream(EMOJI_NUMBER)
                                        .skip(1 + pageCount())
                                        .map(Emoji::fromUnicode)
                                        .filter(emoji -> msg.getReaction(emoji) != null)
                                        .map(msg::removeReaction),
                                // add new reactions
                                emojis.stream()
                                        .filter(emoji -> msg.getReaction(emoji) == null)
                                        .map(msg::addReaction)).toList());
                    });
                }
            }
        }

        @Value
        @NonFinal
        @RequiredArgsConstructor
        public class Adapter$Spigot extends Adapter implements Handler.Minecraft, TabCompleter, CommandExecutor {
            Set<Capability> capabilities = Set.of();
            JavaPlugin      plugin;

            {
                addChild(this);
            }

            @Override
            public List<String> onTabComplete(
                    @NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias,
                    @NotNull String[] args
            ) {
                if (alias.contains(":")) alias = alias.substring(alias.indexOf(':') + 1);
                var strings = strings(alias, args);
                var usage   = createUsageBase(this, strings, expandContext(sender).toArray());
                return autoComplete(usage, String.valueOf(args.length - 1), strings[strings.length - 1]).map(
                        AutoFillOption::key).toList();
            }

            @Override
            public Stream<Object> expandContext(Object... context) {
                return super.expandContext(context).flatMap(expand(it -> {
                    if (it instanceof Player player) return of(player.getUniqueId());
                    return empty();
                }));
            }

            @Override
            public boolean onCommand(
                    @NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label,
                    @NotNull String[] args
            ) {
                if (label.contains(":")) label = label.substring(label.indexOf(':') + 1);
                var strings = strings(label, args);
                var usage   = createUsageBase(this, strings, expandContext(sender).toArray());
                execute(usage, null);
                return true;
            }

            @Override
            public void handleResponse(Usage command, @NotNull Object response, Object... args) {
                if (response instanceof CompletableFuture<?> future) {
                    future.thenAcceptAsync(late -> handleResponse(command, late, args));
                    return;
                }
                var sender = Arrays.stream(args).flatMap(cast(CommandSender.class)).findAny().orElseThrow();
                if (response instanceof Component component) sender.spigot().sendMessage(get().serialize(component));
                else sender.sendMessage(String.valueOf(response));
            }
        }

        public static abstract class Adapter implements Info, Handler {
            @Override
            public void initialize() {
            }

            @Override
            public Stream<Object> expandContext(Object... context) {
                return of(context);
            }

            protected String[] strings(String label, String[] args) {
                var strings = new String[args.length + 1];
                strings[0] = label;
                System.arraycopy(args, 0, strings, 1, args.length);
                return strings;
            }
        }
    }

    @Data
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    abstract class Node implements Named, Described, Aliased, Specifiable<Node> {
        @NotNull String name;

        @Override
        public Stream<String> aliases() {
            return concat(Aliased.super.aliases(), of(getName()));
        }

        @Data
        @SuperBuilder
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        public static abstract class Callable extends Node {
            @NotNull Command attribute;

            public @NotNull String getName() {
                return EmptyAttribute.equals(attribute.value())
                       ? Optional.of(super.getName())
                               .or(() -> Optional.ofNullable(getAlternateName()))
                               .orElseThrow(() -> new NullPointerException("No name defined for command " + this))
                       : attribute.value();
            }

            @Override
            public String getAlternateName() {
                return null; // stub to prevent recursion loop
            }

            public abstract @Nullable Call asCall();

            public abstract Stream<? extends Node> nodes();
        }

        @Value
        @SuperBuilder
        public static class Group extends Callable {
            @NotNull @AnnotatedTarget         Class<?>    source;
            @Singular                         List<Group> groups;
            @Singular                         List<Call>  calls;
            @Nullable @lombok.Builder.Default Call        defaultCall = null;

            @Override
            public @Nullable Call asCall() {
                return defaultCall;
            }

            @Override
            public Stream<Callable> nodes() {
                var stream = concat(groups.stream(), calls.stream());
                if (defaultCall != null) stream = stream.collect(append(defaultCall));
                return stream;
            }

            @Override
            public Wrap<AnnotatedElement> element() {
                return () -> source;
            }
        }

        @Value
        @SuperBuilder
        public static class Call extends Callable {
            @Nullable Object          target;
            @NotNull  Method          method;
            @NotNull  Invocable<?>    callable;
            @Singular List<Parameter> parameters;

            @Override
            public String getAlternateName() {
                return callable.getName();
            }

            @Override
            public @Nullable Call asCall() {
                return this;
            }

            @Override
            public Stream<Parameter> nodes() {
                return parameters.stream().sorted(Parameter.COMPARATOR);
            }

            @Override
            public Wrap<AnnotatedElement> element() {
                return callable::accessor;
            }
        }

        @Value
        @SuperBuilder
        public static class Parameter extends Node implements AutoFillProvider, Default.Extension {
            public static             Comparator<? super Parameter> COMPARATOR = Comparator.comparingInt(param -> param.index);
            @NotNull                  Arg                           attribute;
            @NotNull @AnnotatedTarget java.lang.reflect.Parameter   param;
            boolean required;
            int     index;
            @Singular List<AutoFillProvider> autoFillProviders;

            @Override
            public Stream<String> autoFill(Usage usage, String argName, String currentValue) {
                return autoFillProviders.stream()
                        .filter(Objects::nonNull)
                        .flatMap(provider -> provider.autoFill(usage, argName, currentValue))
                        .distinct();
            }

            @Override
            public Wrap<AnnotatedElement> element() {
                return () -> param;
            }
        }
    }

    @Getter
    class Error extends RuntimeException {
        private final @Nullable Object response;
        private final @Nullable Usage  command;

        public Error(String message) {
            this(message, null);
        }

        public Error(String message, @Nullable Throwable cause) {
            this(message, cause, null);
        }

        public Error(@Nullable String message, @Nullable Throwable cause, @Nullable Object response) {
            this(message, cause, response, null);
        }

        @lombok.Builder
        public Error(
                @Nullable String message, @Nullable Throwable cause, @Nullable Object response,
                @Nullable Usage command
        ) {
            super(Translation.str(message), cause);
            this.response = response;
            this.command  = command;
        }

        public Error(Object response) {
            this(response, null);
        }

        public Error(Object response, @Nullable Throwable cause) {
            this(null, cause, response);
        }
    }
}
