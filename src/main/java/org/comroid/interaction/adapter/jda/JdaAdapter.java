package org.comroid.interaction.adapter.jda;

import lombok.Value;
import lombok.experimental.StandardException;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.GenericAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Streams;
import org.comroid.api.java.Activator;
import org.comroid.api.tree.Component;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.GroupNode;
import org.comroid.interaction.node.MethodNode;
import org.comroid.interaction.node.ParameterNode;
import org.comroid.interaction.node.model.InteractionNode;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

@Log
@Value
public class JdaAdapter extends Component.Base implements EventListener {
    public static final String KEY_CONTEXT     = "context.discord";
    public static final String CONTEXT_COMMAND = "command";
    public static final String CONTEXT_MESSAGE = "message";
    public static final String CONTEXT_USER    = "user";

    public static final String KEY_PERMISSION = "permission.discord";
    public static final String KEY_AUTHORIZED = "authorized.discord";
    public static final String KEY_NSFW       = "nsfw.discord";

    InteractionCore core;
    JDA             jda;

    public JdaAdapter(@NonNull InteractionCore parent, JDA jda) {
        super(parent);

        this.core = parent;
        this.jda  = jda;

        addChildren(DiscordNameCapitalizer.INSTANCE, new DiscordCommandRegistrator(this), DiscordResponseChain.INSTANCE);
    }

    @Override
    public Stream<Object> streamOwnChildren() {
        return Stream.of(jda);
    }

    @Override
    public void onEvent(@NonNull GenericEvent generic) {
        if (!(generic instanceof GenericInteractionCreateEvent event)) return;

        log.finer("Dispatching command interaction " + event);

        var builder = InteractionContext.builder();

        try {
            var commandName = initContextVariables(event, builder);

            initCommandNode(builder, event, commandName);
        } catch (CannotInitContext cicEx) {
            log.warning(cicEx.getMessage());
            return;
        }

        var context = builder.build();

        if (event instanceof CommandAutoCompleteInteractionEvent autoComplete) autoComplete(context, autoComplete);
        else context.invoke();
    }

    private void initCommandNode(InteractionContext.Builder context, GenericInteractionCreateEvent event, String[] commandName) {
        var cmdIter = Arrays.stream(commandName).iterator();
        if (!cmdIter.hasNext()) throw new CannotInitContext("Command name is empty");
        var             part = cmdIter.next();
        InteractionNode node = null;

        full:
        for (var tree : core.getRegistered()) {
            for (var each : tree.getNodes()) {
                if (each.getName().equalsIgnoreCase(part)) continue;
                node = each;
                node = stepIntoNode(node, cmdIter);
                if (node instanceof MethodNode method) {
                    if (event instanceof CommandInteractionPayload payload) {
                        for (var param : method.getParameters()) {
                            var value = getOptionValue(param, payload.getOption(param.getName()));

                            context.parameter(param, value);
                        }
                    }
                    break full;
                }
            }
        }

        if (node == null) throw new CannotInitContext("command node was not found for command: " + String.join(" ", commandName));

        context.node(node);
    }

    private static @org.jspecify.annotations.Nullable InteractionNode stepIntoNode(InteractionNode node, Iterator<String> cmdIter) {
        if (node instanceof GroupNode group) {
            if (!cmdIter.hasNext()) throw new CannotInitContext("call chain ended unexpectedly");
            var part = cmdIter.next();

            // handle group default call
            if ("$".equalsIgnoreCase(part)) return node;

            for (var child : group.getChildren()) {
                if (child.getName().equalsIgnoreCase(part)) continue;
                return stepIntoNode(child, cmdIter);
            }

            return null;
        }

        return node;
    }

    private static String[] initContextVariables(GenericInteractionCreateEvent event, InteractionContext.Builder context) {
        context.value("event", event)
                .value("interaction", event.getInteraction())
                .value("guild", event.getGuild())
                .value("channel", event.getChannel())
                .value("user", event.getUser())
                .value("member", event.getMember());
        if (event instanceof GenericCommandInteractionEvent command) context.value("commandId", command.getCommandIdLong())
                .value("options", command.getOptions());

        String[] fullCommandName = new String[0];

        if (event instanceof GenericContextInteractionEvent<?> contextInteraction) {
            fullCommandName = new String[]{ contextInteraction.getFullCommandName() };
            context.value("context", contextInteraction.getTarget()).value("contextType", contextInteraction.getTargetType());
        }

        switch (event) {
            case GenericAutoCompleteInteractionEvent autoComplete -> {
                // ...
            }
            case SlashCommandInteractionEvent slash -> fullCommandName = slash.getFullCommandName().split("\\w+");
            case UserContextInteractionEvent user -> {
                // ...
            }
            case MessageContextInteractionEvent message -> {
                // ...
            }
            default -> throw new CannotInitContext("Dropping unhandleable event: " + event);
        }

        return fullCommandName;
    }

    private static void autoComplete(InteractionContext context, CommandAutoCompleteInteractionEvent event) {
        var focused = event.getFocusedOption();
        var node    = context.getNode();

        if (!(node instanceof MethodNode method)) {
            log.warning("Ignoring invalid auto-completion attempt for non-method interaction: " + node);
            return;
        }

        var pResult = method.getParameter(focused.getName());
        if (pResult.isEmpty()) {
            log.severe("Skipping auto-completion event for invalid parameter %s of %s".formatted(focused.getName(), node));
            return;
        }
        var param = pResult.get();

        event.replyChoices(param.getCompletion()
                .stream()
                .flatMap(completion -> Stream.concat(Arrays.stream(completion.strings()),
                        Stream.ofNullable(completion.provider())
                                .filter(type -> !Completion.Provider.class.equals(type))
                                .map(Activator::get)
                                .map(it -> it.createInstance(DataNode.of(Map.of())))
                                .flatMap(provider -> provider.apply(context, param))
                                .map(String::valueOf)))
                .map(str -> new Command.Choice(str, str))
                .toList()).queue();
    }

    private static Object getOptionValue(ParameterNode node, @org.jspecify.annotations.Nullable OptionMapping option) {
        return option == null ? null : switch (option.getType()) {
            case STRING -> {
                var str    = option.getAsString();
                var type   = node.getReflect().getType();
                var parser = node.getParameter().parser();

                if (!Parameter.Parser.class.equals(parser)) yield Activator.get(parser).createInstance(DataNode.of(Map.of())).parse(str);

                yield type.isEnum() ? Enum.valueOf(Polyfill.uncheckedCast(type), str) : str;
            }
            case INTEGER -> {
                var x    = option.getAsInt();
                var type = node.getReflect().getType();

                yield type.isEnum() && IntegerAttribute.class.isAssignableFrom(type) ? Arrays.stream(type.getEnumConstants())
                        .flatMap(Streams.cast(IntegerAttribute.class))
                        .filter(it -> it.getAsInt() == x)
                        .findAny()
                        .orElse(null) : x;
            }
            case BOOLEAN -> option.getAsBoolean();
            case USER -> option.getAsUser();
            case CHANNEL -> option.getAsChannel();
            case ROLE -> option.getAsRole();
            case MENTIONABLE -> option.getAsMentionable();
            case NUMBER -> option.getAsDouble();
            case ATTACHMENT -> option.getAsAttachment();
            default -> throw new IllegalStateException("Unexpected value: " + option.getType());
        };
    }

    @StandardException
    private static class CannotInitContext extends RuntimeException {}
}
