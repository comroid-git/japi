package org.comroid.commands.impl;

import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.comroid.annotations.Doc;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.Aliased;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.func.util.Streams;
import org.comroid.api.java.Activator;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.api.tree.Container;
import org.comroid.commands.Command;
import org.comroid.commands.autofill.AutoFillOption;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.autofill.impl.ArrayBasedAutoFillProvider;
import org.comroid.commands.autofill.impl.EnumBasedAutoFillProvider;
import org.comroid.commands.model.CommandCapability;
import org.comroid.commands.model.CommandContextProvider;
import org.comroid.commands.model.CommandError;
import org.comroid.commands.model.CommandInfoProvider;
import org.comroid.commands.model.CommandResponseHandler;
import org.comroid.commands.model.permission.PermissionChecker;
import org.comroid.commands.node.Call;
import org.comroid.commands.node.Callable;
import org.comroid.commands.node.Group;
import org.comroid.commands.node.Node;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.function.Predicate.*;
import static java.util.stream.Stream.*;
import static java.util.stream.Stream.of;
import static org.comroid.api.func.util.Debug.*;
import static org.comroid.api.func.util.Streams.*;

@Value
@Log4j2
@NonFinal
@ToString(of = { "id" })
public class CommandManager extends Container.Base implements CommandInfoProvider {
    public static final CommandResponseHandler DefaultHandler = (command, x, args) -> System.out.println(x);
    UUID      id        = UUID.randomUUID();
    Set<Node> baseNodes = new HashSet<>();

    @Override
    public final Set<CommandCapability> getCapabilities() {
        return streamChildren(CommandCapability.Provider.class).flatMap(provider -> provider.getCapabilities().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("UnusedReturnValue")
    public final Set<Node> register(final Object target) {
        var klass  = target instanceof Class<?> cls0 ? cls0 : target.getClass();
        var groups = new ArrayList<Group>();
        var calls  = new ArrayList<Call>();

        registerGroups(target, groups, klass);
        registerCalls(target, calls, klass);

        Set<Node> nodes;
        var       attr = klass.getAnnotation(Command.class);
        if (attr != null) nodes = Set.of(Group.builder()
                .attribute(attr)
                .name(Command.EmptyAttribute.equals(attr.value()) ? klass.getSimpleName() : attr.value())
                .source(klass)
                .groups(groups)
                .calls(calls)
                .defaultCall(calls.stream().filter(call -> "$".equals(call.name())).findAny().orElse(null))
                .build());
        else nodes = Stream.concat(groups.stream(), calls.stream()).collect(Collectors.toUnmodifiableSet());

        baseNodes.addAll(nodes);
        return nodes;
    }

    @Override
    public final void initialize() {
        streamChildren(AbstractCommandAdapter.class).forEach(AbstractCommandAdapter::initialize);
    }

    public final Stream<AutoFillOption> autoComplete(
            CommandResponseHandler source,
            @Doc("Do not include currentValue") String[] fullCommand,
            String argName, @Nullable String currentValue
    ) {
        var usage = createUsageBase(source, fullCommand);
        return autoComplete(usage, argName, currentValue);
    }

    public final CommandUsage createUsageBase(CommandResponseHandler source, String[] fullCommand, Object... context) {
        var baseNode = baseNodes.stream() // find base node to initiate advancing to execution node
                .filter(node -> node.names().anyMatch(fullCommand[0]::equals))
                .flatMap(cast(Callable.class))
                .findAny()
                .orElseThrow(() -> new CommandError("No such command: " + Arrays.toString(fullCommand)));
        return CommandUsage.builder()
                .source(source)
                .manager(this)
                .fullCommand(trimFullCommand(fullCommand))
                .context(concat(of(this, source), Arrays.stream(context)).flatMap(Streams.expand(it -> children(
                                CommandContextProvider.class).flatMap(ccp -> ccp.expandContext(it))))
                        .collect(Collectors.toUnmodifiableSet()))
                .baseNode(baseNode)
                .build();
    }

    public final Stream<AutoFillOption> autoComplete(
            CommandUsage usage, String argName, @Nullable String currentValue) {
        try {
            // initialize usage
            usage.advanceFull();

            // collect autocompletion stream
            var stackTrace = usage.getStackTrace();
            var paramTrace = usage.getParamTrace();
            var currentCallFirstParam = Stream.of(stackTrace.peek())
                    .filter(callable -> isPermitted(usage, callable))
                    .flatMap(Callable::nodes)
                    .flatMap(cast(org.comroid.commands.node.Parameter.class))
                    .skip(usage.getArgumentStrings().size())
                    .limit(1)
                    .findAny();

            // use current autoFill() when possible
            if (!paramTrace.isEmpty() || currentCallFirstParam.isPresent())
                // if present, try first param of current callable
                return currentCallFirstParam.orElseGet(paramTrace::peek)
                        .autoFill(usage, argName, currentValue)
                        .filter(IAutoFillProvider.stringCheck(currentValue))
                        .map(seq -> seq instanceof AutoFillOption afo
                                    ? afo
                                    : new AutoFillOption(seq.toString(), seq.toString()));
                // else try sub-callables
            else return Stream.of(stackTrace.peek())
                    .filter(callable -> isPermitted(usage, callable))
                    .flatMap(Callable::nodes)
                    .filter(n -> IAutoFillProvider.stringCheck(currentValue).test(n.getName()))
                    .map(n -> new AutoFillOption(n.getName(), n.getDescription()));
        } catch (Throwable e) {
            log.log(isDebug() ? Level.WARN : Level.DEBUG, "An error ocurred during command autocompletion", e);
            return of(usage.getSource().handleThrowable(e)).map(String::valueOf)
                    .map(str -> new AutoFillOption(str, str));
        }
    }

    public final @Nullable Object execute(
            CommandResponseHandler source, String[] fullCommand,
            @Nullable Map<String, Object> namedArgs, Object... extraArgs
    ) {
        var usage = createUsageBase(source, fullCommand, extraArgs);
        return execute(usage, namedArgs);
    }

    public final @Nullable Object execute(CommandUsage usage, @Nullable Map<String, Object> namedArgs) {
        Object result = null, response;
        try {
            usage.advanceFull();

            Call call = usage.getStackTrace().peek().asCall();
            if (call == null) throw new CommandError("No such command");

            validatePermitted(usage, call);

            // sort arguments
            var parameters = call.getParameters();
            var paramIndex = new int[]{ 0 };

            // decide arg handling type
            var argStringSource = (hasCapability(CommandCapability.NAMED_ARGS)
                                   ? (Function<String, Stream<org.comroid.commands.node.Parameter>>) key -> parameters.stream()
                    .filter(p -> p.getName().equals(key))
                                   : (Function<String, Stream<org.comroid.commands.node.Parameter>>) $ -> parameters.stream()
                                           .sorted(Comparator.comparingInt(org.comroid.commands.node.Parameter::getIndex))
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
            result = response = call.getCallable().invoke(call.getTarget(), useArgs);
        } catch (CommandError err) {
            response = err.getResponse() == null ? usage.getSource().handleThrowable(err) : err.getResponse();
        } catch (Throwable e) {
            log.log(isDebug() ? Level.ERROR : Level.DEBUG, "An error ocurred during command execution", e);
            response = usage.getSource().handleThrowable(e);
        }
        if (response != null) usage.getSource().handleResponse(usage, response, usage.getContext().toArray());
        return result;
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CommandManager && obj.hashCode() == hashCode();
    }

    protected Optional<AbstractCommandAdapter> adapter() {
        return streamChildren(AbstractCommandAdapter.class).findAny();
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

    private boolean isPermitted(CommandUsage usage, Callable callable) {
        var permission = callable.getAttribute().permission();
        return usage.getContext()
                       .stream()
                       .flatMap(cast(PermissionChecker.class))
                       .findAny()
                       .isEmpty() || usage.getContext()
                       .stream()
                       .flatMap(cast(PermissionChecker.class))
                       .filter(chk -> chk.acceptPermission(permission))
                       .anyMatch(chk -> chk.userHasPermission(usage, permission));
    }

    private void validatePermitted(CommandUsage usage, Callable callable) {
        if (!isPermitted(usage, callable))
            throw PermissionChecker.insufficientPermissions("missing permission '" + callable.getAttribute()
                    .permission() + "'");
    }

    private Group createGroupNode(@Nullable Object target, Class<?> source) {
        var attribute = Annotations.findAnnotations(Command.class, source).findFirst().orElseThrow().getAnnotation();
        var group = Group.builder()
                .name(Command.EmptyAttribute.equals(attribute.value()) ? source.getSimpleName() : attribute.value())
                .attribute(attribute)
                .source(source)
                .attribute(attribute);

        var groups = new HashSet<Group>();
        registerGroups(target, groups, source);
        group.groups(groups);

        var calls = new HashSet<Call>();
        registerCalls(target, calls, source);
        var defaultCall = calls.stream().filter(call -> "$".equals(call.getName())).findAny().orElse(null);
        if (defaultCall != null) calls.remove(defaultCall);
        group.calls(calls).defaultCall(defaultCall);

        return group.build();
    }

    private Call createCallNode(@Nullable Object target, Method source) {
        var attribute = Annotations.findAnnotations(Command.class, source).findFirst().orElseThrow().getAnnotation();
        var call = Call.builder()
                .name(Command.EmptyAttribute.equals(attribute.value()) ? source.getName() : attribute.value())
                .attribute(attribute)
                .target(target)
                .method(source)
                .callable(Invocable.ofMethodCall(target, source));

        var params = new ArrayList<org.comroid.commands.node.Parameter>();
        registerParameters(params, source);
        params.sort(org.comroid.commands.node.Parameter.COMPARATOR);
        call.parameters(unmodifiableList(params));

        return call.build();
    }

    private void registerGroups(@Nullable Object target, Collection<? super Group> nodes, Class<?> source) {
        for (var groupNodeSource : source.getClasses()) {
            if (!groupNodeSource.isAnnotationPresent(Command.class)) continue;
            var node = createGroupNode(target, groupNodeSource);
            nodes.add(node);
        }
    }

    private org.comroid.commands.node.Parameter createParameterNode(int index, Parameter source) {
        var attribute = Annotations.findAnnotations(Command.Arg.class, source)
                .findFirst()
                .orElseThrow()
                .getAnnotation();
        // construct parameter node
        var builder = org.comroid.commands.node.Parameter.builder()
                .name(Optional.ofNullable(attribute.value())
                        .filter(not(Command.EmptyAttribute::equals))
                        .or(() -> Optional.ofNullable(source.getName()).filter(name -> !name.matches("arg\\d+")))
                        .or(() -> Aliased.$(source).findFirst())
                        .orElse(String.valueOf(index)))
                .attribute(attribute)
                .param(source)
                .required(attribute.required())
                .index(index);

        // init special types
        if (source.getType().isEnum()) builder.autoFillProvider(new EnumBasedAutoFillProvider(Polyfill.uncheckedCast(
                source.getType())));
        else if (attribute.autoFill().length > 0) builder.autoFillProvider(new ArrayBasedAutoFillProvider(attribute.autoFill()));

        // init custom autofill providers
        for (var providerType : attribute.autoFillProvider()) {
            var provider = ReflectionHelper.instanceField(providerType)
                    .stream()
                    .flatMap(cast(IAutoFillProvider.class))
                    .findAny()
                    .orElseGet(() -> Activator.get(providerType).createInstance(JSON.Parser.createObjectNode()));
            builder.autoFillProvider(provider);
        }
        return builder.build();
    }

    private void registerCalls(@Nullable Object target, Collection<? super Call> nodes, Class<?> source) {
        for (var callNodeSource : source.getMethods()) {
            if (!callNodeSource.isAnnotationPresent(Command.class)) continue;
            var node = createCallNode(target, callNodeSource);
            nodes.add(node);
        }
    }

    private void registerParameters(Collection<? super org.comroid.commands.node.Parameter> nodes, Method source) {
        var index = 0;
        for (var paramNodeSource : source.getParameters()) {
            if (!paramNodeSource.isAnnotationPresent(Command.Arg.class)) continue;
            var node = createParameterNode(index, paramNodeSource);
            nodes.add(node);
            index += 1;
        }
    }
}
