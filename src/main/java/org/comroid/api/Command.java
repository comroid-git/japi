package org.comroid.api;

import lombok.*;
import org.comroid.util.StackTraceUtils;
import org.comroid.util.Streams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
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
        String value() default EmptyAttribute;
    }

    interface Handler {
        void handleResponse(Delegate cmd, @NotNull Object response, Object... args);

        default @Nullable String handleThrowable(Throwable throwable) {
            var msg = "%s: %s".formatted(StackTraceUtils.lessSimpleName(throwable.getClass()), throwable.getMessage());
            if (throwable instanceof Error)
                return msg;
            var buf = new StringWriter();
            var out = new PrintWriter(buf);
            out.println(msg);
            Throwable cause = throwable;
            do {
                var c = cause.getCause();
                if (c == null)
                    break;
                cause = c;
            } while (cause instanceof InvocationTargetException
                    || (cause instanceof RuntimeException && cause.getCause() instanceof InvocationTargetException));
            cause.printStackTrace(out);
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

    @Data
    class Manager {
        private final Map<String, Delegate> commands = new ConcurrentHashMap<>();
        private final Handler handler;

        public Manager() {
            this((cmd, x, args) -> System.out.println(x));
        }

        public Manager(Handler handler) {
            register(this.handler = handler);
        }

        @SuppressWarnings("UnusedReturnValue")
        public int register(final Object target) {
            var cls = target.getClass();
            return (int) Arrays.stream(cls.getMethods())
                    .filter(mtd -> mtd.isAnnotationPresent(Command.class))
                    .map(mtd -> {
                        var cmd = Optional.of(mtd.getAnnotation(Command.class));
                        return new Delegate(
                                Invocable.ofMethodCall(target, mtd),
                                cmd.map(Command::value)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElseGet(mtd::getName),
                                cmd.map(Command::ephemeral).orElse(false),
                                cmd.map(Command::usage)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElse(null));
                    })
                    .peek(cmd -> commands.put(cmd.name, cmd))
                    .count();
        }

        public Object execute(String command, Object... extraArgs) {
            var split = command.split(" ");
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
    }

    @Value
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Delegate implements Command, Named {
        Invocable<?> delegate;
        String name;
        boolean ephemeral;
        @Nullable UsageInfo usage;

        private Delegate(Invocable<?> delegate, String name, boolean ephemeral, @Nullable String usage) {
            this.delegate = delegate;
            this.name = name;
            this.ephemeral = ephemeral;
            this.usage = usage == null ? null : parseUsageInfo(usage);
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
