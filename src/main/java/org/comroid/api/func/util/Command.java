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
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.Default;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.*;
import org.comroid.api.data.seri.type.ArrayValueType;
import org.comroid.api.data.seri.type.BoundValueType;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.info.Log;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.comroid.api.func.util.Streams.cast;
import static org.comroid.api.text.Capitalization.lower_hyphen_case;

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

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String value() default EmptyAttribute;

        int index() default -1;

        String[] autoFill() default {};

        Default[] defaultValue() default {};

        boolean required() default true;
    }

    interface Handler {
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
    abstract class Node implements Named, Described, Aliased {
        @Nullable
        @Default
        String name = null;

        public abstract Stream<? extends Node> nodes();

        @Data
        @SuperBuilder
        @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
        public static abstract class CommandNode extends Node {
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
                return null;
            }
        }

        @Value
        @SuperBuilder
        public static class Group extends CommandNode {
            @NotNull
            @AnnotatedTarget
            Class<?> source;
            @Singular
            List<Group> groups;
            @Singular
            List<Call> calls;

            @Override
            public Stream<? extends Node> nodes() {
                return Stream.concat(groups.stream(), calls.stream());
            }
        }

        @Value
        @SuperBuilder
        public static class Call extends CommandNode {
            @NotNull
            Invocable<?> call;
            @Singular
            List<Parameter> parameters;

            @Override
            public Wrap<AnnotatedElement> element() {
                return Wrap.of(call.accessor());
            }

            @Override
            public String getAlternateName() {
                return call.getName();
            }

            @Override
            public Stream<? extends Node> nodes() {
                return parameters.stream();
            }
        }

        @Value
        @SuperBuilder
        public static class Parameter extends Node implements Default.Extension {
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
                return Stream.empty();
            }
        }
    }

    @Value
    @Builder
    class Usage {
        String fullCommand;
        @Singular("context")
        Set<Object> context;
        @Nullable
        @Default
        Handler source = null;
        @NonFinal
        Node node;
    }

    @Value
    @NonFinal
    @ToString(of = {"id"})
    class Manager implements Initializable {
        public static final Handler DefaultHandler = (command, x, args) -> System.out.println(x);
        UUID id = UUID.randomUUID();
        Map<String, Delegate> commands = new ConcurrentHashMap<>();
        Handler handler;
        Set<Adapter> adapters = new HashSet<>();

        public Manager() {
            this(DefaultHandler);
        }

        public Manager(Handler handler) {
            register(this.handler = handler);
        }

        @SuppressWarnings("UnusedReturnValue")
        public final int register(final Object target) {
            var src = target instanceof Class<?> cls0 ? cls0 : target.getClass();
            return (int) Arrays.stream(src.getMethods())
                    .filter(mtd -> mtd.isAnnotationPresent(Command.class))
                    .map(mtd -> {
                        var cmd = Optional.of(mtd.getAnnotation(Command.class));
                        var noArgs = Arrays.stream(mtd.getParameters())
                                .noneMatch(it -> it.isAnnotationPresent(Arg.class));
                        return new Delegate(
                                mtd,
                                target,
                                cmd.map(Command::value)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElseGet(mtd::getName),
                                cmd.map(Command::permission)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElse(""),
                                cmd.map(Command::ephemeral).orElse(false),
                                cmd.map(Command::usage)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElse(null),
                                Arrays.stream(mtd.getParameters())
                                        .filter(it -> noArgs || it.isAnnotationPresent(Arg.class))
                                        .map(param -> {
                                            var arg = Annotations.findAnnotations(Arg.class, param)
                                                    .findAny()
                                                    .map(Annotations.Result::getAnnotation)
                                                    .orElse(null);
                                            return new Arg.Delegate(cmd.get(), param,
                                                    arg != null && arg.required(),
                                                    arg == null ? -1 : arg.index(),
                                                    arg == null ? new String[0] : arg.autoFill());
                                        })
                                        .toList());
                    })
                    .peek(cmd -> Stream.concat(Stream.of(cmd.getName()), cmd.names())
                            .flatMap(Streams.filter(key -> !commands.containsKey(key),
                                    skip -> System.out.println("Skipping command alias " + skip + " because it is a duplicate")))
                            .forEach(key -> commands.put(key, cmd)))
                    .count();
        }

        @Override
        public void initialize() {
            adapters.forEach(Adapter::initialize);
        }

        public final Stream<Map.Entry<String, String>> autoComplete(String fullCommand, String argName, String currentValue) {
            var split = fullCommand.split(" ");
            var name = split[0];
            var cmd = commands.getOrDefault(name, null);
            return cmd.args.stream()
                    .filter(arg -> argName.isBlank() || lower_hyphen_case.convert(arg.name).equals(argName))
                    .flatMap(arg -> arg.autoFill.length == 0 && arg.param.getType().isEnum()
                            ? Arrays.stream(arg.param.getType().getEnumConstants()) // Todo somethings wrong here
                            .flatMap(it -> {
                                var str = Named.$(it);
                                if (!str.startsWith(currentValue))
                                    return Stream.empty();
                                return Stream.of(new AbstractMap.SimpleImmutableEntry<>(str, it.toString()));
                            })
                            : Arrays.stream(arg.autoFill)
                            .map(str -> str.split("[:;=\\s]+]"))
                            .filter(arr -> arr.length > 0)
                            .filter(str -> str[0].startsWith(currentValue))
                            .map(parts -> parts.length == 2
                                    ? new AbstractMap.SimpleImmutableEntry<>(parts[0], parts[1])
                                    : new AbstractMap.SimpleImmutableEntry<>(parts[0], parts[0])));
        }

        public final Object execute(String fullCommand, Object... extraArgs) {
            var split = fullCommand.split(" ");
            var name = split[0];
            var cmd = commands.getOrDefault(name, null);
            var args = adapter().map(adapter -> adapter.expandArgs(cmd, Arrays.stream(split).skip(1).toList(), extraArgs))
                    .orElseGet(() -> {
                        var map = new HashMap<String, Object>();
                        boolean first = true;
                        int c = 1;
                        for (var each : split) {
                            if (first) {
                                first = false;
                                continue;
                            }
                            if (map.put("arg"+c++, each) != null) {
                                throw new IllegalStateException("Duplicate key");
                            }
                        }
                        return map;
                    });
            args.put("args", Arrays.stream(split).skip(1).toArray(String[]::new));
            Throwable thr = null;
            Object response = null;
            try {
                if ("help".equals(name) && !commands.containsKey("help")) {
                    var sb = new StringBuilder("Commands");
                    for (var each : commands.values()) {
                        sb.append("\n\t- ").append(each.name);
                        if (each.usage != null)
                            sb.append(' ').append(each.usage.hint);
                    }
                    response = sb.toString();
                } else {
                    if (cmd == null)
                        throw new Error("Command not found: " + name);
                    Object result;
                    try {
                        result = cmd.execute(args, extraArgs);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                    if (result instanceof Error)
                        throw (Error) result;
                    if (result != null)
                        response = result;
                }
            } catch (Error e) {
                response = e.getClass().getSimpleName() + ": " + e.getMessage();
            } catch (Throwable t) {
                assert cmd != null;
                thr = new RuntimeException("A fatal error occurred during execution of command " + cmd.getName(), t);
            }
            var handler = handler();
            if (thr != null)
                response = handler.handleThrowable(thr);
            if (response == null)
                response = "✅";
            handler.handleResponse(cmd, response, Stream.of(args)
                    .collect(Streams.append(Stream.of(args.values().stream()
                            .map(Object::toString)
                            .toArray(String[]::new))))
                    .collect(Streams.append(extraArgs))
                    .toArray());
            return response;
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
                                            .map(e -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(e.getKey(), e.getValue()))
                                            .toList())
                                    .queue();
                        });

                jda.updateCommands().addCommands(
                        commands.values().stream()
                                .map(cmd -> {
                                    final var slash = Commands.slash(cmd.name, cmd.getDescription().isBlank()?"No description":cmd.getDescription());
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
                ).queue();
                initialized = true;
            }

            @Override
            public void handleResponse(Command.Delegate cmd, @NotNull Object response, Object... args) {
                final var e = Stream.of(args)
                        .flatMap(cast(SlashCommandInteractionEvent.class))
                        .findAny()
                        .orElseThrow();
                final var user = Stream.of(args)
                        .flatMap(cast(User.class))
                        .findAny()
                        .orElseThrow();
                if (response instanceof CompletableFuture)
                    e.deferReply().setEphemeral(cmd.ephemeral())
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
                    req.setEphemeral(cmd.ephemeral()).submit();
                }
            }

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
