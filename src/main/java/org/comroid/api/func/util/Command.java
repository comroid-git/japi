package org.comroid.api.func.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
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
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.type.ArrayValueType;
import org.comroid.api.data.seri.type.BoundValueType;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.Specifiable;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.info.Constraint;
import org.comroid.api.info.Log;
import org.comroid.api.java.Activator;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Capitalization;
import org.comroid.api.text.StringMode;
import org.comroid.api.tree.Container;
import org.comroid.api.tree.Initializable;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.function.Function.*;
import static java.util.function.Predicate.*;
import static java.util.stream.Stream.of;
import static java.util.stream.Stream.*;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.*;
import static org.comroid.api.func.util.Streams.*;

@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Command {
    String EmptyAttribute = "@@@";

    String value() default EmptyAttribute;

    String usage() default EmptyAttribute;

    @SuppressWarnings("LanguageMismatch")
    @Language(value = "Groovy", prefix = "Object x =", suffix = ";")
    String permission() default EmptyAttribute;

    boolean ephemeral() default false;

    enum Capability {
        NAMED_ARGS;

        @FunctionalInterface
        public interface Provider {
            default boolean hasCapability(Capability capability) {
                return getCapabilities().stream().anyMatch(capability::equals);
            }

            Set<Capability> getCapabilities();
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
        PermissionChecker DENY_ALL = (usage, key) -> false;

        static PermissionChecker minecraft(Adapter adapter) {
            return (usage, key) -> {
                var userId = usage.getContext().stream()
                        .flatMap(Streams.cast(UUID.class))
                        .findAny().orElseThrow();
                return key instanceof Integer level
                       ? adapter.checkOpLevel(userId, level)
                       : adapter.checkPermission(userId, key.toString(), false)
                               .toBooleanOrElse(false);
            };
        }

        static Error insufficientPermissions(@Nullable String detail) {
            return new Error("You dont have permission to do this " + (detail == null ? "\b" : detail));
        }

        default boolean userHasPermission(Usage usage) {
            return getPermissionKey(usage)
                    .filter(key -> userHasPermission(usage, key))
                    .isPresent();
        }

        default Optional<Object> getPermissionKey(Usage usage) {
            return Optional.of(usage.getNode().getAttribute().permission())
                    .filter(Predicate.<String>not(Command.EmptyAttribute::equals)
                            .and(not(String::isBlank)))
                    .map(StandardValueType::findGoodType);
        }

        boolean userHasPermission(Usage usage, Object key);

        interface Adapter {
            default boolean checkOpLevel(UUID playerId) {
                return checkOpLevel(playerId, 1);
            }

            boolean checkOpLevel(UUID playerId, @MagicConstant(intValues = { 0, 1, 2, 3, 4 }) int minimum);

            TriState checkPermission(UUID playerId, String key, boolean explicit);
        }
    }

    @FunctionalInterface
    interface AutoFillProvider {
        Stream<String> autoFill(Usage usage, String argName, String currentValue);

        enum Duration implements AutoFillProvider {
            @Instance INSTANCE;

            @Override
            public Stream<String> autoFill(Usage usage, String argName, String currentValue) {
                var chars = currentValue.toCharArray();

                if (!currentValue.isEmpty()) {
                    var last = chars[chars.length - 1];
                    if (Character.isDigit(last))
                        return of("m", "h", "d", "w", "mo", "y")
                                .distinct()
                                .map(suffix -> currentValue + suffix);
                }
                return of("5m", "6h", "3d", "2w", "1y");
            }
        }

        @Value
        class Array implements AutoFillProvider {
            String[] options;

            public Array(String... options) {
                this.options = options;
            }

            @Override
            public Stream<String> autoFill(Usage usage, String argName, String currentValue) {
                return of(options);
            }
        }

        @Value
        class Enum implements AutoFillProvider {
            Class<? extends java.lang.Enum<?>> type;

            @Override
            public Stream<String> autoFill(Usage usage, String argName, String currentValue) {
                return Arrays.stream(type.getEnumConstants())
                        .map(Named::$)
                        .map(usage.source.getDesiredKeyCapitalization()::convert);
            }
        }
    }

    @FunctionalInterface
    interface Handler {
        default Capitalization getDesiredKeyCapitalization() {
            return Capitalization.lower_case;
        }

        void handleResponse(Usage command, @NotNull Object response, Object... args);

        default String handleThrowable(Throwable throwable) {
            while (throwable instanceof InvocationTargetException itex)
                throwable = throwable.getCause();
            var msg = "%s: %s".formatted(
                    throwable instanceof Error
                    ? throwable.getClass().getSimpleName()
                    : StackTraceUtils.lessSimpleName(throwable.getClass()),
                    throwable.getMessage() == null
                    ? throwable.getCause() == null
                      ? "Internal Error"
                      : throwable.getCause() instanceof Error
                        ? throwable.getCause().getMessage()
                        : throwable.getCause().getClass().getSimpleName() + ": " + throwable.getCause().getMessage()
                    : throwable.getMessage());
            if (throwable instanceof Error)
                return msg;
            var buf = new StringWriter();
            var out = new PrintStream(new DelegateStream.Output(buf));
            out.println(msg);
            Throwable cause = throwable;
            do {
                var c = cause.getCause();
                if (c == null)
                    break;
                cause = c;
            } while (cause instanceof InvocationTargetException
                    || (cause instanceof RuntimeException && cause.getCause() instanceof InvocationTargetException));
            StackTraceUtils.wrap(cause, out, true);
            var str = buf.toString();
            if (str.length() > 1950)
                str = str.substring(0, 1950);
            return str;
        }

        interface Minecraft extends Handler {
            @Override
            default String handleThrowable(Throwable throwable) {
                return "§c" + Handler.super.handleThrowable(throwable);
            }
        }
    }

    interface Info extends Capability.Provider, ContextProvider, Initializable {
    }

    record AutoFillOption(String key, String description) {
    }

    @Value
    @lombok.Builder
    class Usage {
        Manager manager;
        String[] fullCommand;
        @Singular("context")
        Set<Object> context;
        @NotNull
        Handler     source;
        @lombok.Builder.Default
        Node.Callable baseNode = null;
        @NonFinal
        Node.Callable node;
        @NonFinal
        int         callIndex;

        public void advanceFull() {
            // reset if necessary
            if (callIndex != 0) {
                node = baseNode;
                callIndex = 0;
            }
            // start from i=1 because the initial node was spawned at creation in Manager#createUsageBase()
            for (var i = 1; i < fullCommand.length; i++) {
                if (node instanceof Node.Call) // do not advance into parameters
                    break;
                var text = fullCommand[i];
                var result = node.nodes()
                        .filter(it -> it.names().anyMatch(text::equals))
                        .flatMap(cast(Node.Callable.class))
                        .findAny();
                if (result.isEmpty())
                    break;
                node = result.get();
                callIndex = i;
            }
        }
    }

    @Value
    @NonFinal
    @ToString(of = { "id" })
    class Manager extends Container.Base implements Info, PermissionChecker {
        public static final Handler DefaultHandler = (command, x, args) -> System.out.println(x);
        UUID id = UUID.randomUUID();
        Set<Node> baseNodes = new HashSet<>();

        @Override
        public final Set<Capability> getCapabilities() {
            return streamChildren(Capability.Provider.class)
                    .flatMap(provider -> provider.getCapabilities().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @SuppressWarnings("UnusedReturnValue")
        public final Set<Node> register(final Object target) {
            var klass = target instanceof Class<?> cls0 ? cls0 : target.getClass();
            var nodes = new HashSet<Node>();

            registerGroups(target, nodes, klass);
            registerCalls(target, nodes, klass);

            baseNodes.addAll(nodes);
            return nodes;
        }

        public final Node.Group createGroupNode(@Nullable Object target, Class<?> source) {
            var attribute = Annotations.findAnnotations(Command.class, source)
                    .findFirst().orElseThrow().getAnnotation();
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
            var defaultCall = calls.stream()
                    .filter(call -> "$".equals(call.getName()))
                    .findAny().orElse(null);
            if (defaultCall != null)
                calls.remove(defaultCall);
            group.calls(calls).defaultCall(defaultCall);

            return group.build();
        }

        public final Node.Call createCallNode(@Nullable Object target, Method source) {
            var attribute = Annotations.findAnnotations(Command.class, source)
                    .findFirst().orElseThrow().getAnnotation();
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
                String argName,
                @Nullable String currentValue
        ) {
            var usage = createUsageBase(source, fullCommand);
            return autoComplete(usage, argName, currentValue);
        }

        protected final Usage createUsageBase(Handler source, String[] fullCommand, Object... baseArgs) {
            var baseNode = baseNodes.stream() // find base node to initiate advancing to execution node
                    .filter(node -> node.names().anyMatch(fullCommand[0]::equals))
                    .flatMap(cast(Node.Callable.class))
                    .findAny().orElseThrow(() -> new Error("No such command: " + Arrays.toString(fullCommand)));
            return Usage.builder()
                    .source(source)
                    .manager(this)
                    .fullCommand(fullCommand)
                    .context(expandContext(concat(Stream.of(this, source), Arrays.stream(baseArgs)).toArray())
                            .collect(Collectors.toSet()))
                    .baseNode(baseNode)
                    .node(baseNode)
                    .build();
        }

        @Override
        public final Stream<Object> expandContext(Object... baseContext) {
            return streamChildren(ContextProvider.class).flatMap(multiply(provider -> provider.expandContext(baseContext)));
        }

        public final Stream<AutoFillOption> autoComplete(Usage usage, String argName, @Nullable String currentValue) {
            try {
                usage.advanceFull();
                verifyPermission(usage);

                return (usage.node instanceof Node.Call call
                        ? call.nodes()
                                .skip(usage.callIndex + usage.fullCommand.length - 2)
                                .limit(1)
                                .flatMap(param -> param.autoFill(usage, argName, currentValue))
                        : usage.node.nodes().map(Node::getName))
                        .map(String::trim)
                        .distinct()
                        .filter(not("$"::equals))
                        .filter(str -> {
                            var current = currentValue == null ? "" : currentValue;
                            return str.toLowerCase().startsWith(current.toLowerCase());
                        })
                        .map(str -> new AutoFillOption(str, str));
            } catch (Throwable e) {
                Log.at(Level.WARNING, "An error ocurred during command execution", e);
                return Stream.of(usage.source.handleThrowable(e))
                        .map(String::valueOf)
                        .map(str -> new AutoFillOption(str, ""));
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
            return streamChildren(PermissionChecker.class)
                    .findFirst()
                    .filter(pc -> pc.userHasPermission(usage))
                    .isPresent();
        }

        public final @Nullable Object execute(
                Handler source,
                String[] fullCommand,
                @Nullable Map<String, Object> namedArgs,
                Object... extraArgs
        ) {
            var usage = createUsageBase(source, fullCommand, extraArgs);
            return execute(usage, namedArgs);
        }

        public final @Nullable Object execute(Usage usage, @Nullable Map<String, Object> namedArgs) {
            Object result = null, response;
            try {
                usage.advanceFull();
                verifyPermission(usage);

                Node.Call call;
                if (usage.node instanceof Node.Group group) {
                    call = group.defaultCall;
                    //usage.callIndex += 1;
                } else call = usage.node.as(Node.Call.class, "Invalid node type! Is your syntax correct?");
                if (call == null)
                    throw new Error("No such command");

                // sort arguments
                if (usage.callIndex < 0 || usage.callIndex >= usage.fullCommand.length)
                    throw new Error("No such command: " + String.join(" ", usage.fullCommand));
                var      parameterTypes = call.callable.parameterTypesOrdered();
                Object[] useArgs        = new Object[parameterTypes.length];
                var      useNamedArgs   = hasCapability(Capability.NAMED_ARGS);
                var      argIndex       = usage.callIndex + 1;
                for (int i = 0; i < useArgs.length; i++) {
                    final int i0            = i;
                    var       parameterType = parameterTypes[i];
                    var parameter = Stream.of(call.callable.accessor())
                            .flatMap(cast(Method.class))
                            .map(mtd -> mtd.getParameters()[i0])
                            .findAny();
                    var attribute = parameter
                            .flatMap(param -> Annotations.findAnnotations(Arg.class, param).findFirst())
                            .map(Annotations.Result::getAnnotation)
                            .orElse(null);
                    Node.Parameter paramNode = null;
                    if (attribute != null)
                        paramNode = call.parameters.stream()
                                .filter(node -> node.attribute.equals(attribute))
                                .findAny().orElseThrow();
                    if (attribute == null) {
                        // try to fit in an extraArg
                        useArgs[i] = usage.context.stream()
                                .filter(parameterType::isInstance)
                                .findAny()
                                .orElseGet(() -> {
                                    if (parameterType.isArray() && parameterType.getComponentType().equals(String.class)) {
                                        var args = new String[usage.fullCommand.length - usage.callIndex - 1];
                                        System.arraycopy(usage.fullCommand, usage.callIndex + 1, args, 0, args.length);
                                        return args;
                                    } else return null;
                                });
                    } else if (useNamedArgs) {
                        // eg. discord, fabric
                        Constraint.notNull(namedArgs, "args").run();
                        Constraint.notNull(paramNode, "parameter").run();
                        if (paramNode.isRequired() && !namedArgs.containsKey(paramNode.getName()))
                            throw new Error("Missing argument " + paramNode.getName());

                        final var finalParamNode = paramNode;
                        useArgs[i] = Optional.ofNullable(namedArgs.get(paramNode.getName()))
                                .or(() -> usage.context.stream()
                                        .flatMap(cast(finalParamNode.param.getType()))
                                        .findAny())
                                .or(() -> Optional.ofNullable(finalParamNode.defaultValue())
                                        .map(Polyfill::uncheckedCast))
                                .orElse(null);
                    } else {
                        // eg. console, minecraft
                        Constraint.notNull(paramNode, "parameter").run();
                        if (paramNode.isRequired() && argIndex >= usage.fullCommand.length)
                            throw new Error("Not enough arguments");
                        String argStr;
                        if (argIndex < usage.fullCommand.length) {
                            if (attribute.stringMode() == StringMode.GREEDY) {
                                var buf = new String[usage.fullCommand.length - argIndex];
                                System.arraycopy(usage.fullCommand, argIndex, buf, 0, buf.length);
                                argStr = String.join(" ", buf);
                            } else argStr = usage.fullCommand[argIndex];
                        } else argStr = null;

                        argIndex += 1;
                        var valueType = ValueType.of(parameterType);
                        useArgs[i] = argStr == null ? valueType.defaultValue() : valueType.parse(argStr);
                    }
                }

                result = response = call.callable.invoke(call.target, useArgs);
            } catch (Error err) {
                response = err.response == null ? usage.source.handleThrowable(err) : err.response;
            } catch (Throwable e) {
                Log.at(Level.FINE, "An error ocurred during command execution", e);
                response = usage.source.handleThrowable(e);
            }
            if (response != null)
                usage.source.handleResponse(usage, response, usage.context.toArray());
            return result;
        }

        protected Optional<Adapter> adapter() {
            return streamChildren(Adapter.class).findAny();
        }

        private void registerGroups(@Nullable Object target, Collection<? super Node.Group> nodes, Class<?> source) {
            for (var groupNodeSource : source.getClasses()) {
                if (!groupNodeSource.isAnnotationPresent(Command.class))
                    continue;
                var node = createGroupNode(target, groupNodeSource);
                nodes.add(node);
            }
        }

        private Node.Parameter createParameterNode(int index, Method origin, Parameter source) {
            var attribute = Annotations.findAnnotations(Arg.class, source)
                    .findFirst().orElseThrow().getAnnotation();
            // construct parameter node
            var builder = Node.Parameter.builder()
                    .name(Optional.ofNullable(attribute.value())
                            .filter(not(EmptyAttribute::equals))
                            .or(() -> Optional.ofNullable(source.getName())
                                    .filter(name -> !name.matches("arg\\d+")))
                            .or(() -> Aliased.$(source).findFirst())
                            .orElse(String.valueOf(index)))
                    .attribute(attribute)
                    .param(source)
                    .required(attribute.required())
                    .index(index);

            // init special types
            if (source.getType().isEnum())
                builder.autoFillProvider(new AutoFillProvider.Enum(Polyfill.uncheckedCast(source.getType())));
            else if (attribute.autoFill().length > 0)
                builder.autoFillProvider(new AutoFillProvider.Array(attribute.autoFill()));

            // init custom autofill providers
            for (var providerType : attribute.autoFillProvider()) {
                var provider = ReflectionHelper.instanceField(providerType).stream()
                        .flatMap(cast(AutoFillProvider.class))
                        .findAny()
                        .orElseGet(() -> Activator.get(providerType).createInstance(DataNode.Value.NULL));
                builder.autoFillProvider(provider);
            }
            return builder.build();
        }

        private void registerCalls(@Nullable Object target, Collection<? super Node.Call> nodes, Class<?> source) {
            for (var callNodeSource : source.getMethods()) {
                if (!callNodeSource.isAnnotationPresent(Command.class))
                    continue;
                var node = createCallNode(target, callNodeSource);
                nodes.add(node);
            }
        }

        private void registerParameters(Collection<? super Node.Parameter> nodes, Method source) {
            var index = 0;
            for (var paramNodeSource : source.getParameters()) {
                if (!paramNodeSource.isAnnotationPresent(Arg.class))
                    continue;
                var node = createParameterNode(index, source, paramNodeSource);
                nodes.add(node);
                index += 1;
            }
        }

        @Value
        @RequiredArgsConstructor
        public class Adapter$JDA extends Adapter {
            Set<Capability>         capabilities = Set.of(Capability.NAMED_ARGS);
            JDA                     jda;
            Event.Bus<GenericEvent> bus          = new Event.Bus<>();
            @Nullable
            @NonFinal
            @Setter
            BiFunction<EmbedBuilder, User, EmbedBuilder> embedFinalizer = null;
            @NonFinal
            boolean initialized = false;

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
                bus.flatMap(SlashCommandInteractionEvent.class).listen()
                        .subscribeData(event -> execute(Adapter$JDA.this,
                                event.getCommandString().split(" "),
                                Map.of(),
                                event.getName(),
                                event,
                                event.getUser(),
                                event.getGuild(),
                                event.getChannel()));
                bus.flatMap(CommandAutoCompleteInteractionEvent.class).listen()
                        .subscribeData(event -> {
                            var option = event.getFocusedOption();
                            event.replyChoices(autoComplete(Adapter$JDA.this,
                                            event.getCommandString().split(" "),
                                            option.getName(),
                                            option.getValue())
                                            .map(e -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(e.key, e.description))
                                            .toList())
                                    .queue();
                        });

                /* todo
                jda.updateCommands().addCommands(
                        baseNodes.values().stream()
                                .map(cmd -> {
                                    final var slash = Commands.slash(cmd.getName(), cmd.getDescription().isBlank()?"No description":cmd.getDescription());
                                    for (var arg : cmd.args) {
                                        final var isEnumArg = arg.param.getType().isEnum();
                                        OptionAdapter.of(arg.param.getType())
                                                .or(() -> isEnumArg && LongAttribute.class.isAssignableFrom(arg.param.getType()) ? OptionAdapter.Long : null)
                                                .or(() -> isEnumArg && IntegerAttribute.class.isAssignableFrom(arg.param.getType()) ? OptionAdapter.Int : null)
                                                .or(() -> isEnumArg ? OptionAdapter.String : null)
                                                .ifPresent(adp -> slash.addOption(
                                                        adp.getOptionType(),
                                                        lower_hyphen_case.convert(arg.name),
                                                        cmd.getDescription().isBlank()?"No description":cmd.getDescription(),
                                                        arg.required,
                                                        arg.autoFill.length > 0 || isEnumArg));
                                    }
                                    if (cmd.permission.matches("-?\\d+")) {
                                        var permissions = cmd.parsePermission(StandardValueType.LONG);
                                        var perms = DefaultMemberPermissions.enabledFor(permissions);
                                        slash.setDefaultPermissions(perms);
                                    }
                                    return slash;
                                })
                                .toList()
                ).queue();*/
                initialized = true;
            }

            @Override
            public void handleResponse(Usage cmd, @NotNull Object response, Object... args) {
                final var e = Stream.of(args)
                        .flatMap(cast(SlashCommandInteractionEvent.class))
                        .findAny()
                        .orElseThrow();
                final var user = Stream.of(args)
                        .flatMap(cast(User.class))
                        .findAny()
                        .orElseThrow();
                if (response instanceof CompletableFuture)
                    e.deferReply().setEphemeral(cmd.node.attribute.ephemeral())
                            .submit()
                            .thenCombine(((CompletableFuture<?>) response), (hook, resp) -> {
                                WebhookMessageCreateAction<Message> req;
                                if (resp instanceof EmbedBuilder embed) {
                                    if (embedFinalizer != null)
                                        embed = embedFinalizer.apply(embed, user);
                                    req = hook.sendMessageEmbeds(embed.build());
                                } else req = hook.sendMessage(String.valueOf(resp));
                                return req.submit();
                            })
                            .thenCompose(identity())
                            .exceptionally(Polyfill.exceptionLogger());
                else {
                    ReplyCallbackAction req;
                    if (response instanceof EmbedBuilder embed) {
                        if (embedFinalizer != null)
                            embed = embedFinalizer.apply(embed, user);
                        req = e.replyEmbeds(embed.build());
                    } else req = e.reply(String.valueOf(response));
                    req.setEphemeral(cmd.node.attribute.ephemeral()).submit();
                }
            }

            /*
            @Override
            protected Map<String, Object> expandArgs(Delegate cmd, List<String> args, Object[] extraArgs) {
                var event = Stream.of(extraArgs)
                        .flatMap(cast(SlashCommandInteractionEvent.class))
                        .findAny().orElseThrow();
                var map = new HashMap<String, Object>();
                event.getOptions().stream()
                        .flatMap(option -> {
                            var arg = cmd.args.stream()
                                    .filter(it -> lower_hyphen_case.convert(it.name).equals(option.getName()))
                                    .findAny()
                                    .orElse(null);
                            if (arg == null)
                                return Stream.empty();
                            final var isEnumArg = arg.param.getType().isEnum();
                            return OptionAdapter.of(arg.param.getType())
                                    .or(() -> isEnumArg && LongAttribute.class.isAssignableFrom(arg.param.getType()) ? OptionAdapter.Long.new Enum<>(arg.param.getType()) : null)
                                    .or(() -> isEnumArg && IntegerAttribute.class.isAssignableFrom(arg.param.getType()) ? OptionAdapter.Int.new Enum<>(arg.param.getType()) : null)
                                    .or(() -> isEnumArg ? OptionAdapter.String.new Enum<>(arg.param.getType()) : null)
                                    .map(adp -> new AbstractMap.SimpleImmutableEntry<>(arg.name, adp.getFrom(option)))
                                    .stream();
                        })
                        .forEach(e->map.put(e.getKey(),e.getValue()));
                return map;
            }
            */

            @Getter
            @RequiredArgsConstructor
            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            public enum OptionAdapter implements IOptionAdapter, Named {
                Boolean(StandardValueType.BOOLEAN, OptionType.BOOLEAN) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsBoolean();
                    }
                },
                Int(StandardValueType.INTEGER, OptionType.INTEGER) {
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
                },
                Long(StandardValueType.LONG, OptionType.INTEGER) {
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
                },
                Double(StandardValueType.DOUBLE, OptionType.NUMBER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsDouble();
                    }
                },
                String(StandardValueType.STRING, OptionType.STRING) {
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
                },
                Attachment(BoundValueType.of(Message.Attachment.class), OptionType.ATTACHMENT) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsAttachment();
                    }
                },
                Mentionable(BoundValueType.of(IMentionable.class), OptionType.MENTIONABLE) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsMentionable();
                    }
                },
                Channel(BoundValueType.of(GuildChannelUnion.class), OptionType.CHANNEL) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsChannel();
                    }
                },
                Role(BoundValueType.of(net.dv8tion.jda.api.entities.Role.class), OptionType.ROLE) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsRole();
                    }
                },
                User(BoundValueType.of(User.class), OptionType.USER) {
                    @Override
                    public Object getFrom(OptionMapping option) {
                        return option.getAsUser();
                    }
                },
                Member(BoundValueType.of(net.dv8tion.jda.api.entities.Member.class), OptionType.USER) {
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
        }

        @Value
        @NonFinal
        @RequiredArgsConstructor
        public class Adapter$Spigot extends Adapter implements Handler.Minecraft, TabCompleter, CommandExecutor {
            Set<Capability> capabilities = Set.of();
            JavaPlugin plugin;

            {
                addChild(this);
            }

            @Override
            public List<String> onTabComplete(
                    @NotNull CommandSender sender,
                    @NotNull org.bukkit.command.Command command,
                    @NotNull String alias,
                    @NotNull String[] args
            ) {
                if (alias.contains(":"))
                    alias = alias.substring(alias.indexOf(':') + 1);
                var strings = strings(alias, args);
                var usage = createUsageBase(this, strings, expandContext(sender).toArray());
                return autoComplete(usage, String.valueOf(args.length - 1), strings[strings.length - 1])
                        .map(AutoFillOption::key)
                        .toList();
            }

            @Override
            public Stream<Object> expandContext(Object... context) {
                return super.expandContext(context).flatMap(expand(it -> {
                    if (it instanceof Player player)
                        return Stream.of(player.getUniqueId());
                    return empty();
                }));
            }

            @Override
            public boolean onCommand(
                    @NotNull CommandSender sender,
                    @NotNull org.bukkit.command.Command command,
                    @NotNull String label,
                    @NotNull String[] args
            ) {
                if (label.contains(":"))
                    label = label.substring(label.indexOf(':') + 1);
                var strings = strings(label, args);
                var usage = createUsageBase(this, strings, expandContext(sender).toArray());
                execute(usage, null);
                return true;
            }

            @Override
            public void handleResponse(Usage command, @NotNull Object response, Object... args) {
                if (response instanceof CompletableFuture<?> future) {
                    future.thenAcceptAsync(late -> handleResponse(command, late, args));
                    return;
                }
                var sender = Arrays.stream(args)
                        .flatMap(cast(CommandSender.class))
                        .findAny().orElseThrow();
                if (response instanceof Component component)
                    sender.spigot().sendMessage(get().serialize(component));
                else sender.sendMessage(String.valueOf(response));
            }
        }

        public static abstract class Adapter implements Info, Handler {
            @Override
            public void initialize() {
            }

            @Override
            public Stream<Object> expandContext(Object... context) {
                return Stream.of(context);
            }

            protected String[] strings(String label, String[] args) {
                var strings = new String[args.length + 1];
                strings[0] = label;
                System.arraycopy(args, 0, strings, 1, args.length);
                return strings;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Manager && obj.hashCode() == hashCode();
        }

        @Override
        public final int hashCode() {
            return id.hashCode();
        }
    }

    @Data
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    abstract class Node implements Named, Described, Aliased, Specifiable<Node> {
        @NotNull
        String name;

        @Override
        public Stream<String> aliases() {
            return Stream.concat(Aliased.super.aliases(), of(getName()));
        }

        @Data
        @SuperBuilder
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        public static abstract class Callable extends Node {
            @NotNull
            Command attribute;

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

            public abstract Stream<? extends Node> nodes();
        }

        @Value
        @SuperBuilder
        public static class Group extends Callable {
            @NotNull
            @AnnotatedTarget
            Class<?>   source;
            @Singular
            List<Group> groups;
            @Singular
            List<Call> calls;
            @Nullable
            @lombok.Builder.Default
            Call       defaultCall = null;

            @Override
            public Stream<Callable> nodes() {
                var stream = Stream.concat(groups.stream(), calls.stream());
                if (defaultCall != null) stream = stream.collect(Streams.append(defaultCall));
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
            @Nullable
            Object       target;
            @NotNull
            Method       method;
            @NotNull
            Invocable<?> callable;
            @Singular
            List<Parameter> parameters;

            @Override
            public String getAlternateName() {
                return callable.getName();
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
            public static Comparator<? super Parameter> COMPARATOR = Comparator.comparingInt(param -> param.index);
            @NotNull
            Arg attribute;
            @NotNull
            @AnnotatedTarget
            java.lang.reflect.Parameter param;
            boolean required;
            int index;
            @Singular
            List<AutoFillProvider> autoFillProviders;

            @Override
            public Stream<String> autoFill(Usage usage, String argName, String currentValue) {
                return autoFillProviders.stream()
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
        private final @Nullable Usage command;

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
        public Error(@Nullable String message, @Nullable Throwable cause, @Nullable Object response, @Nullable Usage command) {
            super(message, cause);
            this.response = response;
            this.command = command;
        }

        public Error(Object response) {
            this(response, null);
        }

        public Error(Object response, @Nullable Throwable cause) {
            this(null, cause, response);
        }
    }
}
