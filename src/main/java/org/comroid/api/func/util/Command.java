package org.comroid.api.func.util;

import lombok.*;
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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.Default;
import org.comroid.annotations.Doc;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.*;
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
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.tree.Initializable;
import org.intellij.lang.annotations.Language;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;
import static java.util.function.Predicate.not;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.comroid.api.func.util.Streams.cast;

@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    String EmptyAttribute = "@@@";

    String value() default EmptyAttribute;

    String usage() default EmptyAttribute;

    @Language(value = "Groovy", prefix = "Object x =", suffix = ";")
    String permission() default EmptyAttribute;

    boolean ephemeral() default false;

    enum Capability {
        NAMED_ARGS;

        public interface Provider {
            default Stream<Capability> capabilities() {
                return Stream.empty();
            }

            default boolean hasCapability(Capability capability) {
                return capabilities().anyMatch(capability::equals);
            }
        }
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String value() default EmptyAttribute;

        int index() default -1;

        String[] autoFill() default {};

        Default[] defaultValue() default {};

        boolean required() default true;
    }

    interface Handler extends Capability.Provider {
        void handleResponse(Usage command, @NotNull Object response, Object... args);

        default @Nullable String handleThrowable(Throwable throwable) {
            Log.get().log(Level.WARNING, "Exception occurred in command", throwable);
            var msg = "%s: %s".formatted(StackTraceUtils.lessSimpleName(throwable.getClass()), throwable.getMessage());
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
    }

    @Data
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    abstract class Node implements Named, Described, Aliased, Specifiable<Node> {
        @NotNull
        String name;

        public abstract Stream<? extends Node> nodes();

        @Override
        public Stream<String> names() {
            return Stream.concat(of(getName()), Aliased.super.names());
        }

        @Data
        @SuperBuilder
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        public static abstract class Callable extends Node {
            @NotNull
            Command attribute;

            public @Nullable String getName() {
                return EmptyAttribute.equals(attribute.value())
                        ? Optional.ofNullable(super.getName())
                        .or(() -> Optional.ofNullable(getAlternateName()))
                        .orElseThrow(() -> new NullPointerException("No name defined for command " + this))
                        : attribute.value();
            }

            @Override
            public String getAlternateName() {
                return null; // stub to prevent recursion loop
            }
        }

        @Value
        @SuperBuilder
        public static class Group extends Callable {
            @NotNull
            @AnnotatedTarget
            Class<?> source;
            @Singular
            List<Group> groups;
            @Singular
            List<Call> calls;
            @Nullable
            @lombok.Builder.Default
            Call defaultCall = null;

            @Override
            public Stream<? extends Node> nodes() {
                var stream = Stream.concat(groups.stream(), calls.stream());
                if (defaultCall != null) stream = stream.collect(Streams.append(defaultCall));
                return stream;
            }
        }

        @Value
        @SuperBuilder
        public static class Call extends Callable {
            @Nullable
            Object target;
            @NotNull
            Method method;
            @NotNull
            Invocable<?> callable;
            @Singular
            List<Parameter> parameters;

            @Override
            public Wrap<AnnotatedElement> element() {
                return Wrap.of(callable.accessor());
            }

            @Override
            public String getAlternateName() {
                return callable.getName();
            }

            @Override
            public Stream<? extends Node> nodes() {
                return parameters.stream().sorted(Parameter.COMPARATOR);
            }
        }

        @Value
        @SuperBuilder
        public static class Parameter extends Node implements Default.Extension {
            public static Comparator<? super Parameter> COMPARATOR = Comparator.comparingInt(param -> param.index);
            @NotNull
            Arg attribute;
            @NotNull
            @AnnotatedTarget
            java.lang.reflect.Parameter param;
            boolean required;
            int index;
            String[] autoFill;

            @Override
            public Stream<? extends Node> nodes() {
                return of(autoFill)
                        .map(str -> ParameterAutoFill.builder()
                                .name(str)
                                .build());
            }
        }

        @Value
        @SuperBuilder
        private static class ParameterAutoFill extends Node {
            @NotNull
            @lombok.Builder.Default
            String description = "no description";

            @Override
            public Stream<? extends Node> nodes() {
                return Stream.empty();
            }
        }
    }

    record AutoCompletionOption(String key, String description) {
    }

    @Value
    @Builder
    class Usage {
        Manager manager;
        String[] fullCommand;
        @Singular("context")
        Set<Object> context;
        @Nullable
        @Default
        Handler source = null;
        @NonFinal
        Node.Callable node;

        public void advanceFull() {
            // start from i=1 because the initial node was spawned at creation in Manager#createUsageBase()
            for (var i = 1; i < fullCommand.length; i++) {
                var text = fullCommand[i];
                if (node instanceof Node.Call) // do not advance into parameters
                    break;
                var result = node.nodes()
                        .filter(it -> it.names().anyMatch(text::equals))
                        .flatMap(cast(Node.Callable.class))
                        .findAny();
                if (result.isEmpty())
                    break;
                node = result.get();
            }
        }
    }

    @Value
    @NonFinal
    @ToString(of = {"id"})
    class Manager implements Capability.Provider, Initializable {
        public static final Handler DefaultHandler = (command, x, args) -> System.out.println(x);
        UUID id = UUID.randomUUID();
        Handler handler;
        Set<Node> baseNodes = new HashSet<>();
        Set<Adapter> adapters = new HashSet<>();

        public Manager() {
            this(DefaultHandler);
        }

        public Manager(Handler handler) {
            register(this.handler = handler);
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

        private void registerGroups(@Nullable Object target, Collection<? super Node.Group> nodes, Class<?> source) {
            for (var groupNodeSource : source.getClasses()) {
                if (!groupNodeSource.isAnnotationPresent(Command.class))
                    continue;
                var node = createGroupNode(target, groupNodeSource);
                nodes.add(node);
            }
        }

        public Node.Group createGroupNode(@Nullable Object target, Class<?> source) {
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

        private void registerCalls(@Nullable Object target, Collection<? super Node.Call> nodes, Class<?> source) {
            for (var callNodeSource : source.getMethods()) {
                if (!callNodeSource.isAnnotationPresent(Command.class))
                    continue;
                var node = createCallNode(target, callNodeSource);
                nodes.add(node);
            }
        }

        public Node.Call createCallNode(@Nullable Object target, Method source) {
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

        private Node.Parameter createParameterNode(int index, Method origin, Parameter source) {
            var attribute = Annotations.findAnnotations(Arg.class, source)
                    .findFirst().orElseThrow().getAnnotation();
            return Node.Parameter.builder()
                    .name(Optional.ofNullable(attribute.value())
                            .filter(not(EmptyAttribute::equals))
                            .or(() -> Optional.ofNullable(source.getName())
                                    .filter(name -> !name.matches("arg\\d+")))
                            .or(() -> Aliased.$(source).findFirst())
                            .orElse(String.valueOf(index)))
                    .attribute(attribute)
                    .param(source)
                    .required(!attribute.required()
                            && !source.isAnnotationPresent(NotNull.class) && source.isAnnotationPresent(Nullable.class))
                    .index(index)
                    .autoFill(attribute.autoFill())
                    .build();
        }

        @Override
        public void initialize() {
            adapters.forEach(Adapter::initialize);
        }

        @Deprecated
        public final Stream<AutoCompletionOption> autoComplete(@Doc("Do not include currentValue") String fullCommand, String argName, String currentValue) {
            return autoComplete(fullCommand.split(" "), argName, currentValue);
        }

        public final Stream<AutoCompletionOption> autoComplete(@Doc("Do not include currentValue") String[] fullCommand, String argName, String currentValue) {
            var usage = createUsageBase(fullCommand);
            usage.advanceFull();
            if (!(usage.node instanceof Node.Call call))
                return usage.node.nodes().map(Node::getName)
                        .filter(not("$"::equals))
                        .map(str -> new AutoCompletionOption(str, ""));
            Node.Parameter parameter;
            if (hasCapability(Capability.NAMED_ARGS)) {
                // eg. discord
                var any = call.parameters.stream()
                        .filter(p -> argName.equals(p.getName()))
                        .findAny();
                if (any.isEmpty())
                    return empty();
                parameter = any.get();
            } else {
                // eg. minecraft
                // assume all parameter names as their indices
                var callIndex = Arrays.binarySearch(fullCommand, call.getName());
                if (callIndex < 0 || callIndex >= fullCommand.length)
                    throw new Error("Internal error computing argument position");
                var paramIndex = fullCommand.length - callIndex; // already offset bcs of length
                if (paramIndex > call.parameters.size())
                    return empty();
                parameter = call.parameters.get(paramIndex);
            }
            return of(parameter.autoFill)
                    .map(str -> new AutoCompletionOption(str, ""));
        }

        @Deprecated
        public final Object execute(String fullCommand, Object... extraArgs) {
            return execute(fullCommand.split(" "), null, extraArgs);
        }

        public final Object execute(String[] fullCommand, @Nullable Map<String, Object> namedArgs, Object... extraArgs) throws Error, ArgumentError {
            var usage = createUsageBase(fullCommand);
            usage.advanceFull();

            Node.Call call;
            if (usage.node instanceof Node.Group group)
                call = group.defaultCall;
            else call = usage.node.as(Node.Call.class, "Invalid node type! Is your syntax correct?");
            if (call == null)
                throw new Error("No such command");

            // sort arguments
            var parameterTypes = call.callable.parameterTypesOrdered();
            Object[] useArgs = new Object[parameterTypes.length];
            var useNamedArgs = hasCapability(Capability.NAMED_ARGS);
            var callIndex = IntStream.range(0, fullCommand.length)
                    .filter(i -> fullCommand[i].equals(call.getName()))
                    .findFirst().orElse(-1);
            //var callIndex = Arrays.binarySearch(fullCommand, call.getName());
            if (callIndex < 0 || callIndex >= fullCommand.length)
                throw new Error("Internal error");
            for (int i = 0; i < useArgs.length; i++) {
                final int i0 = i;
                var parameterType = parameterTypes[i];
                var parameter = Stream.of(call.callable.accessor())
                        .flatMap(Streams.cast(Method.class))
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
                    useArgs[i] = Arrays.stream(extraArgs)
                            .filter(parameterType::isInstance)
                            .findAny()
                            .orElseGet(() -> {
                                if (parameter.stream().flatMap(Aliased::$).anyMatch("args"::equals)
                                        && parameterType.isArray()
                                        && parameterType.getComponentType().equals(String.class)) {
                                    var args = new String[fullCommand.length - callIndex - 1];
                                    System.arraycopy(fullCommand, callIndex + 1, args, 0, args.length);
                                    return args;
                                } else return null;
                            });
                } else if (useNamedArgs) {
                    // eg. discord
                    Constraint.notNull(namedArgs, "args").run();
                    Constraint.notNull(paramNode, "parameter").run();
                    if (paramNode.isRequired() && !namedArgs.containsKey(paramNode.getName()))
                        throw new ArgumentError("Missing argument " + paramNode.getName());

                    useArgs[i] = namedArgs.get(paramNode.getName());
                } else {
                    // eg. console, minecraft
                    Constraint.notNull(paramNode, "parameter").run();
                    var argIndex = callIndex + i;
                    if (paramNode.isRequired() && argIndex > fullCommand.length)
                        throw new ArgumentError("Not enough arguments");
                    var argStr = fullCommand[argIndex];

                    useArgs[i] = StandardValueType.forClass(parameterType)
                            .map(svt -> (Object) svt.parse(argStr))
                            .orElseGet(() -> Activator.get(parameterType)
                                    .createInstance(DataNode.of(argStr)));
                }
            }

            try {
                return call.callable.invoke(call.target, useArgs);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new Error(usage, "Internal execution Error", e, new String[0]);
            }
        }

        private Usage createUsageBase(String[] fullCommand, Object... extraArgs) {
            return Usage.builder()
                    .manager(this)
                    .fullCommand(fullCommand)
                    .context(of(extraArgs).collect(Collectors.toSet())) // set should be modifiable
                    .node(baseNodes.stream() // find base node to initiate advancing to execution node
                            .filter(node -> node.names().anyMatch(fullCommand[0]::equals))
                            .flatMap(Streams.cast(Node.Callable.class))
                            .findAny().orElseThrow(() -> new Error("No such command: " + Arrays.toString(fullCommand))))
                    .build();
        }

        protected Optional<Adapter> adapter() {
            return adapters.stream().findAny();
        }

        protected Handler handler() {
            return adapter()
                    .map(Polyfill::<Handler>uncheckedCast)
                    .orElse(handler);
        }

        @Override
        public final int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Manager && obj.hashCode() == hashCode();
        }

        @Override
        public Stream<Capability> capabilities() {
            return adapters.stream()
                    .flatMap(Capability.Provider::capabilities)
                    .collect(Streams.append(handler.capabilities()));
        }

        public static abstract class Adapter implements Handler, Initializable {
            @Override
            public void initialize() {}

            protected @Nullable Map<String, Object> expandArgs(Delegate cmd, List<String> args, Object[] extraArgs) {
                return null;
            }
        }

        @Value
        @RequiredArgsConstructor
        public class Adapter$JDA extends Adapter {
            JDA jda;
            Event.Bus<GenericEvent> bus = new Event.Bus<>();
            @Nullable
            @NonFinal
            @Setter
            BiFunction<EmbedBuilder, User, EmbedBuilder> embedFinalizer = null;
            @NonFinal boolean initialized = false;

            {
                adapters.add(this);
            }

            @Override
            public Stream<Capability> capabilities() {
                return of(Capability.NAMED_ARGS);
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
                        .subscribeData(event -> execute(event.getName(), event, event.getUser(), event.getGuild(), event.getChannel()));
                bus.flatMap(CommandAutoCompleteInteractionEvent.class).listen()
                        .subscribeData(event -> {
                            var option = event.getFocusedOption();
                            event.replyChoices(autoComplete(event.getName(), option.getName(), option.getValue())
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
                final var e = of(args)
                        .flatMap(cast(SlashCommandInteractionEvent.class))
                        .findAny()
                        .orElseThrow();
                final var user = of(args)
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
                            .thenCompose(Function.identity())
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

            public interface IOptionAdapter {
                ValueType<?> getValueType();
                OptionType getOptionType();
                Object getFrom(OptionMapping option);
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

                ValueType<?> valueType;
                OptionType optionType;

                public static Wrap<IOptionAdapter> of(final Class<?> type) {
                    return Wrap.of(Arrays.stream(values())
                            .filter(adp -> adp.valueType.getTargetClass().isAssignableFrom(type))
                            .findAny());
                }

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
                                .filter(it->attribute(it).contentEquals(value))
                                .findAny()
                                .orElseThrow(() -> new NoSuchElementException("Invalid enum value: " + value));
                    }
                }
            }
        }

        @Value
        @RequiredArgsConstructor
        public class Adapter$Spigot extends Adapter {
            JavaPlugin plugin;

            {
                adapters.add(this);
            }

            @Override
            public void handleResponse(Usage command, @NotNull Object response, Object... args) {
                var message = response instanceof CompletableFuture<?> future ? future.join() : response;
                var sender = Arrays.stream(args)
                        .flatMap(cast(CommandSender.class))
                        .findAny().orElseThrow();
                sender.sendMessage(String.valueOf(message));
            }

            @Override
            public @Nullable String handleThrowable(Throwable throwable) {
                return ChatColor.RED + super.handleThrowable(throwable);
            }
        }
    }

    @Getter
    @Setter
    class Error extends RuntimeException {
        private Usage command;
        private String[] args;

        public Error(String message) {
            this(null, message, null);
        }

        public Error(Usage command, String message, String[] args) {
            super(message);
            this.command = command;
            this.args = args;
        }

        public Error(Usage command, String message, Throwable cause, String[] args) {
            super(message, cause);
            this.command = command;
            this.args = args;
        }
    }

    class ArgumentError extends Error {
        public ArgumentError(String message) {
            super(message);
        }

        public ArgumentError(String nameof, @Nullable String detail) {
            super("Invalid argument '" + nameof + "'" + Optional.ofNullable(detail).map(d -> "; " + d).orElse(""));
        }
    }
}
