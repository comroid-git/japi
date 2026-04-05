package org.comroid.interaction.adapter.jda;

import lombok.Value;
import lombok.experimental.StandardException;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
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
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.data.RegExpUtil;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.util.Streams;
import org.comroid.api.java.Activator;
import org.comroid.api.text.Capitalization;
import org.comroid.api.tree.Component;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.component.NameCapitalizer;
import org.comroid.interaction.component.response.ResponseChain;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.model.Response;
import org.comroid.interaction.node.GroupNode;
import org.comroid.interaction.node.MethodNode;
import org.comroid.interaction.node.ParameterNode;
import org.comroid.interaction.node.model.InteractionNode;
import org.comroid.interaction.node.model.InvokableNode;
import org.comroid.util.JdaUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Log
@Value
public class JdaAdapter extends Component.Base
        implements EventListener, NameCapitalizer, ResponseChain<MessageCreateData> {
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
    }

    @Override
    public Class<MessageCreateData> getResponseType() {
        return MessageCreateData.class;
    }

    @Override
    public void deferResponse(InteractionContext context) {
        var privacy = privacy(context);
        if (privacy == Interaction.PrivacyLevel.PRIVATE) return;

        context.child(IReplyCallback.class).assertion().deferReply().setEphemeral(privacy == Interaction.PrivacyLevel.EPHEMERAL).map(context::addChild).queue();
    }

    @Override
    public void sendResponse(InteractionContext context, MessageCreateData response) {
        var privacy = privacy(context);

        if (privacy == Interaction.PrivacyLevel.PRIVATE) {
            var result = context.child(User.class);
            if (result.isNull()) {
                log.warning("Dropping response because there is no message target compatible to the set privacy level %s: %s".formatted(privacy, response));
                return;
            }

            result.assertion().openPrivateChannel().flatMap(channel -> channel.sendMessage(response)).queue();

            return;
        }

        if (async(context)) {
            var deferredReply = context.child(InteractionHook.class);
            if (deferredReply.isNonNull()) {
                deferredReply.assertion().editOriginal(JdaUtil.convertToEditData(response)).queue();
                return;
            }
        } else {
            var directReply = context.child(IReplyCallback.class);
            if (directReply.isNonNull()) {
                directReply.assertion().reply(response).setEphemeral(privacy == Interaction.PrivacyLevel.EPHEMERAL).queue();
                return;
            }
        }

        if (privacy != Interaction.PrivacyLevel.PUBLIC) {
            log.warning("Dropping response because there is no message target compatible to the set privacy level %s: %s".formatted(privacy, response));
            return;
        }

        context.child(MessageChannelUnion.class)
                .ifPresentOrElse(channel -> channel.sendMessage(response).queue(),
                        () -> log.warning("Dropping response because there is no message target: " + response));
    }

    @Override
    public @Nullable MessageCreateData convertResponse(Object object) {
        if (object instanceof Response response) return convertResponse(response);
        if (object instanceof EmbedBuilder builder) object = builder.build();
        if (object instanceof MessageEmbed embed) object = new MessageCreateBuilder().addEmbeds(embed);
        if (object instanceof MessageCreateBuilder builder) object = builder.build();
        if (object instanceof MessageCreateData data) return data;
        return null;
    }

    @Override
    public MessageCreateData convertResponse(Response response) {
        if (!response.isValid()) throw new IllegalArgumentException("invalid response: " + response);

        var message = new MessageCreateBuilder();

        if (response.isPlaintext()) message.setContent(response.content());
        else message.addEmbeds(response.toEmbed().build());

        return message.build();
    }

    @Override
    public Stream<Object> streamOwnChildren() {
        return Stream.of(jda);
    }

    /// register commands with discord
    @Override
    protected void $initialize() {
        var all = new ArrayList<CommandData>();

        for (var tree : core.getRegistered()) {
            for (var base : tree.getNodes()) {
                CommandData data;
                var         ctx = base.getDefinitionValues(KEY_CONTEXT).findAny().orElse(CONTEXT_COMMAND);

                if (base instanceof GroupNode group) {
                    if (!ctx.equalsIgnoreCase(CONTEXT_COMMAND)) {
                        log.severe("Cannot register interaction with discord; groups are only allowed with slash commands; " + base);
                        continue;
                    }

                    // only slash commands can be grouped
                    data = Commands.slash(getCapitalization(ComponentType.CALLABLE).convert(base.getName()),
                            base.getDescription() != null ? base.getDescription() : InteractionCore.NO_DESCRIPTION);

                    for (var child : group.getChildren()) {
                        var subcommand = createSubcommand(child);
                        ((SlashCommandData) data).addSubcommands(subcommand);
                    }
                } else if (base instanceof MethodNode method) data = switch (ctx) {
                    case CONTEXT_COMMAND -> {
                        var slash = Commands.slash(getCapitalization(ComponentType.CALLABLE).convert(base.getName()),
                                base.getDescription() != null ? base.getDescription() : InteractionCore.NO_DESCRIPTION);

                        for (var param : method.getParameters()) slash.addOptions(createOptionData(param));

                        yield slash;
                    }
                    case CONTEXT_MESSAGE -> Commands.message(getCapitalization(ComponentType.TITLE).convert(base.getName()));
                    case CONTEXT_USER -> Commands.user(getCapitalization(ComponentType.TITLE).convert(base.getName()));
                    default -> throw new IllegalStateException("Unexpected value: " + ctx);
                };
                else throw new IllegalStateException("Unexpected value: " + base);

                initCommandData(base, data);
                all.add(data);
            }
        }

        log.info("Upserting %d interactions to discord bot %s".formatted(all.size(), jda.getSelfUser()));

        RestAction<?> action = new CompletedRestAction<>(jda, (Object) null);
        for (var data : all) action = action.flatMap($ -> jda.upsertCommand(data));
        action.queue();
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

    @Override
    public Capitalization getCapitalization(ComponentType ignored) {
        return ignored == ComponentType.TITLE ? Capitalization.Title_Case : Capitalization.lower_hyphen_case;
    }

    private String[] initContextVariables(GenericInteractionCreateEvent event, InteractionContext.Builder context) {
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

    private @Nullable InteractionNode stepIntoNode(InteractionNode node, Iterator<String> cmdIter) {
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

    private Interaction.PrivacyLevel privacy(InteractionContext context) {
        return context.child(Interaction.class).map(Interaction::privacy).orElse(Interaction.PrivacyLevel.EPHEMERAL);
    }

    private boolean async(InteractionContext context) {
        return context.child(Interaction.class).filter(Interaction::async).isNonNull();
    }

    private void autoComplete(InteractionContext context, CommandAutoCompleteInteractionEvent event) {
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

    private Object getOptionValue(ParameterNode node, @Nullable OptionMapping option) {
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

    private static void initCommandData(InteractionNode node, CommandData data) {
        initDefaultPermission(node.getFilterValues(KEY_PERMISSION), data);
        initNsfw(node, data);
    }

    private static void initDefaultPermission(Stream<String> permissions, CommandData data) {
        var permissionMask = permissions.flatMap(perm -> Optional.ofNullable(Permission.valueOf(perm.toUpperCase()))
                        .map(Stream::of)
                        .orElseGet(() -> Stream.of(perm)
                                .filter(it -> it.matches("\\d+"))
                                .mapToLong(Long::parseLong)
                                .mapToObj(Permission::getPermissions)
                                .flatMap(Collection::stream)))
                .mapToLong(Permission::getRawValue)
                .collect(AtomicLong::new, (a, x) -> a.accumulateAndGet(x, (l, r) -> l | r), (l, r) -> l.accumulateAndGet(r.get(), (ln, rn) -> ln | rn))
                .get();
        data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissionMask));
    }

    private static void initNsfw(InteractionNode node, CommandData data) {
        data.setNSFW(node.getDefinitionValues(KEY_NSFW).anyMatch(str -> str.toLowerCase().matches(RegExpUtil.TRUE_PATTERN)));
    }

    private static SubcommandData createSubcommand(InvokableNode node) {
        return switch (node) {
            case GroupNode ignored -> throw new IllegalStateException("GroupNode may not be a subcommand: " + node);
            case MethodNode method -> new SubcommandData(method.getName(),
                    method.getDescription() != null ? method.getDescription() : InteractionCore.NO_DESCRIPTION).addOptions(method.getParameters()
                    .stream()
                    .map(JdaAdapter::createOptionData)
                    .toList());
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };
    }

    private static @NonNull OptionData createOptionData(ParameterNode param) {
        return new OptionData(getOptionType(param),
                param.getName(),
                param.getDescription() != null ? param.getDescription() : InteractionCore.NO_DESCRIPTION,
                param.getParameter().required(),
                !param.getCompletion().isEmpty() || param.getReflect().getType().isEnum());
    }

    private static OptionType getOptionType(ParameterNode node) {
        var pType = node.getReflect().getType();
        pType = ValueType.of(pType).getTargetClass();

        if (pType.isEnum()) {
            if (IntegerAttribute.class.isAssignableFrom(pType)) return OptionType.INTEGER;
            return OptionType.STRING;
        }

        if (Boolean.class.isAssignableFrom(pType)) return OptionType.BOOLEAN;
        if (Number.class.isAssignableFrom(pType)) return OptionType.NUMBER;
        if (UserSnowflake.class.isAssignableFrom(pType)) return OptionType.USER;
        if (Role.class.isAssignableFrom(pType)) return OptionType.ROLE;
        if (Channel.class.isAssignableFrom(pType)) return OptionType.CHANNEL;
        if (IMentionable.class.isAssignableFrom(pType)) return OptionType.MENTIONABLE;
        if (Message.Attachment.class.isAssignableFrom(pType)) return OptionType.ATTACHMENT;
        return OptionType.STRING;
    }

    @StandardException
    private static class CannotInitContext extends RuntimeException {}
}
