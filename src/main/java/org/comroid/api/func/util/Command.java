package org.comroid.api.func.util;

import lombok.*;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.comroid.annotations.Default;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Named;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.tree.Initializable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    String EmptyAttribute = "@@@";

    String value() default EmptyAttribute;

    String usage() default EmptyAttribute;

    boolean ephemeral() default false;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String[] autoFill() default {};

        Default[] defaultValue() default {};

        @Value
        class Delegate implements Named {
            Command cmd;
            String name;
            Parameter param;
            @Nullable Object defaultValue;

            public Delegate(Command cmd, Parameter param) {
                this.cmd = cmd;
                this.param = param;
                this.name = Annotations.aliases(param).stream().findAny().orElseGet(param::getName);
                this.defaultValue = Annotations.defaultValue(param);
            }
        }
    }

    interface Handler {
        void handleResponse(Delegate cmd, @NotNull Object response, Object... args);

        default @Nullable String handleThrowable(Throwable throwable) {
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
            if (str.length()>1950)
                str=str.substring(0,1950);
            return str;
        }
    }

    @Getter
    @Setter
    class Error extends RuntimeException {
        private Delegate command;
        private String[] args;

        public Error(String message) {
            this(null, message, null);
        }

        public Error(Delegate command, String message, String[] args) {
            super(message);
            this.command = command;
            this.args = args;
        }

        public Error(Delegate command, String message, Throwable cause, String[] args) {
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

    @Value
    @NonFinal
    class Manager implements Initializable {
        public static final Handler DefaultHandler = (cmd, x, args) -> System.out.println(x);
        UUID id = UUID.randomUUID();
        Map<String, Delegate> commands = new ConcurrentHashMap<>();
        Handler handler;
        @Nullable Adapter adapter;

        public Manager() {
            this(DefaultHandler);
        }

        public Manager(Handler handler) {
            this(handler, null);
        }

        @lombok.Builder
        public Manager(Handler handler, @Nullable Adapter adapter) {
            register(this.handler = handler);
            this.adapter = adapter;
        }

        @SuppressWarnings("UnusedReturnValue")
        public final int register(final Object target) {
            var cls = target.getClass();
            return (int) Arrays.stream(cls.getMethods())
                    .filter(mtd -> mtd.isAnnotationPresent(Command.class))
                    .map(mtd -> {
                        var cmd = Optional.of(mtd.getAnnotation(Command.class));
                        var noArgs = Arrays.stream(mtd.getParameters())
                                .noneMatch(it -> it.isAnnotationPresent(Arg.class));
                        return new Delegate(
                                Invocable.ofMethodCall(target, mtd),
                                cmd.map(Command::value)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElseGet(mtd::getName),
                                cmd.map(Command::ephemeral).orElse(false),
                                cmd.map(Command::usage)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElse(null),
                                Arrays.stream(mtd.getParameters())
                                        .filter(it -> noArgs || it.isAnnotationPresent(Arg.class))
                                        .map(param -> new Arg.Delegate(cmd.get(), param))
                                        .toList());
                    })
                    .peek(cmd -> commands.put(cmd.name, cmd))
                    .count();
        }

        @Override
        public void initialize() throws Throwable {
            if (adapter != null)
                adapter.initialize();
        }

        public final Stream<Map.Entry<String, String>> autoComplete(String fullCommand, String argName, String currentValue) {
            var split = fullCommand.split(" ");
            var name = split[0];
            var cmd = commands.getOrDefault(name, null);
            cmd.args.stream()
                    .filter(arg->arg.name.equals(argName))
                    .findAny()
        }

        public final Object execute(String fullCommand, Object... extraArgs) {
            var split = fullCommand.split(" ");
            var name = split[0];
            var cmd = commands.getOrDefault(name, null);
            var args = Arrays.stream(split).skip(1).toArray(String[]::new);
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
            if (thr != null)
                response = handler.handleThrowable(thr);
            if (response != null)
                handler.handleResponse(cmd, response, Stream.of(args).collect(Streams.append(extraArgs)).toArray());
            return response;
        }

        @Override
        public final int hashCode() {
            return id.hashCode();
        }

        public abstract class Adapter implements Initializable {
            public abstract Stream<Map.Entry<String, String>> autoComplete(String fullCommand, String argName, String currentValue);

            public abstract Object execute(String fullCommand, Object... extraArgs);

            @Value
            class JDA extends Adapter {
                net.dv8tion.jda.api.JDA jda;

                @Override
                public void initialize() throws Throwable {
                    jda.updateCommands().addCommands(
                            commands.values().stream()
                                    .map(cmd -> Commands.slash(cmd.name, Annotations.descriptionText(cmd.delegate.accessor())))
                                    .toList()
                    ).queue();
                }

                @Override
                public Stream<Map.Entry<String, String>> autoComplete(String fullCommand, String argName, String currentValue) {
                    return null;
                }

                @Override
                public Object execute(String fullCommand, Object... extraArgs) {
                    return null;
                }
            }
        }
    }

    @Value
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Delegate implements Command, Named {
        Invocable<?> delegate;
        String name;
        boolean ephemeral;
        @Nullable UsageInfo usage;
        List<Arg.Delegate> args;

        private Delegate(Invocable<?> delegate, String name, boolean ephemeral, @Nullable String usage, List<Arg.Delegate> args) {
            this.delegate = delegate;
            this.name = name;
            this.ephemeral = ephemeral;
            this.usage = usage == null ? null : parseUsageInfo(usage);
            this.args = args;
        }

        @Override
        public String value() {
            return name;
        }

        @Override
        @Nullable
        public String usage() {
            return usage != null ? usage.hint : null;
        }

        @Override
        public boolean ephemeral() {
            return ephemeral;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Command.class;
        }

        private Object execute(String[] args, Object... extraArgs) throws InvocationTargetException, IllegalAccessException, InstantiationException {
            if (usage != null)
                usage.validate(args);
            return delegate.invokeAutoOrder(Stream.of(this, args).collect(Streams.append(extraArgs)).toArray());
        }

        private UsageInfo parseUsageInfo(String usage) {
            var split = usage.split(" ");
            return new UsageInfo(usage,
                    (int) Arrays.stream(split).filter(s -> s.startsWith("<")).count(),
                    split.length,
                    Arrays.stream(split).skip(split.length - 1).anyMatch(s -> s.contains("..")));
        }

        @Value
        private class UsageInfo {
            String hint;
            int required;
            int total;
            boolean ellipsis;

            private void validate(String[] args) {
                if (args.length < required)
                    throw new Error("not enough arguments; usage: " + name + " " + hint);
                if (!ellipsis && args.length > total)
                    throw new Error("too many arguments; usage: " + name + " " + hint);
            }
        }
    }
}
