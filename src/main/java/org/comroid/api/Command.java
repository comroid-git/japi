package org.comroid.api;

import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    String EmptyAttribute = "@@@";

    String value() default EmptyAttribute;

    String usage() default EmptyAttribute;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String value() default EmptyAttribute;
    }

    interface Handler {
        void handleResponse(String text);
        void handleError(Error error);
    }

    @Value
    class Error extends RuntimeException {
        @With Delegate cmd;
        @With String[] args;

        public Error(String message) {
            this(null, message, null);
        }

        public Error(Delegate cmd, String message, String[] args) {
            super(message);
            this.cmd = cmd;
            this.args = args;
        }

        public Error(Delegate cmd, String message, Throwable cause, String[] args) {
            super(message, cause);
            this.cmd = cmd;
            this.args = args;
        }
    }

    @Data
    class Manager {
        private final Map<String, Delegate> commands = new ConcurrentHashMap<>();
        private final Handler handler;

        public Manager() {
            this(new Command.Handler(){
                @Override
                public void handleResponse(String text) {
                    System.out.println(text);
                }

                @Override
                public void handleError(Error error) {
                    error.printStackTrace(System.err);
                }
            });
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
                                cmd.map(Command::usage)
                                        .filter(x -> !EmptyAttribute.equals(x))
                                        .orElse(null));
                    })
                    .peek(cmd -> commands.put(cmd.name, cmd))
                    .count();
        }

        public @Nullable String execute(String command, Object... extraArgs) {
            var split = command.split(" ");
            var name = split[0];
            var cmd = commands.get(name);
            var args = Arrays.stream(split).skip(1).toArray(String[]::new);
            Error error = null;
            try {
                String str;
                if ("help".equals(name) && !commands.containsKey("help")) {
                    var sb = new StringBuilder("Commands");
                    for (var each : commands.values())
                        sb.append("\n\t- ").append(each.name);
                    str = sb.toString();
                } else {
                    var result = cmd.execute(args, extraArgs);
                    if (result instanceof Error)
                        throw (Error)result;
                    str = String.valueOf(result);
                }

                handler.handleResponse(str);
                return str;
            } catch (Error e) {
                error = e;
                if (e.getCmd() == null)
                    error = e.withCmd(cmd);
                if (e.getArgs() == null)
                    error = e.withArgs(args);
            } catch (Throwable t) {
                error = new Error(cmd, "A fatal error occurred during command execution", t, args);
            }
            handler.handleError(error);
            return null;
        }
    }

    @Value
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Delegate implements Command, Named {
        Invocable<?> delegate;
        String name;
        @Nullable UsageInfo usage;

        private Delegate(Invocable<?> delegate, String name, @Nullable String usage) {
            this.delegate = delegate;
            this.name = name;
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
        public Class<? extends Annotation> annotationType() {
            return Command.class;
        }

        private Object execute(String[] args, Object... extraArgs) {
            if (usage != null)
                usage.validate(args);
            return delegate.autoInvoke(Stream
                    .concat(Arrays.stream(extraArgs), Stream.of(this, args))
                    .toArray());
        }

        private UsageInfo parseUsageInfo(String usage) {
            var split = usage.split(" ");
            return new UsageInfo(usage,
                    (int) Arrays.stream(split).filter(s->s.startsWith("<")).count(),
                    Arrays.stream(split).anyMatch(s->s.contains("..")) ? Integer.MAX_VALUE : split.length);
        }

        @Value
        private class UsageInfo {
            String hint;
            int required;
            int maximum;

            private void validate(String[] args) {
                if (args.length < required)
                    throw new Error(Delegate.this, "not enough arguments; usage: " + name + " " + hint, args);
                if (args.length > maximum)
                    throw new Error(Delegate.this, "too many arguments; usage: " + name + " " + hint, args);
            }
        }
    }
}
