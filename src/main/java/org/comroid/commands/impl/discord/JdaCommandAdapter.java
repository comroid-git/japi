package org.comroid.commands.impl.discord;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
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
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.attr.LongAttribute;
import org.comroid.api.attr.Named;
import org.comroid.api.attr.StringAttribute;
import org.comroid.api.data.seri.type.ArrayValueType;
import org.comroid.api.data.seri.type.BoundValueType;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.Event;
import org.comroid.api.tree.UncheckedCloseable;
import org.comroid.commands.impl.AbstractCommandAdapter;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandCapability;
import org.comroid.commands.model.CommandPrivacyLevel;
import org.comroid.commands.model.permission.PermissionChecker;
import org.comroid.commands.node.Call;
import org.comroid.commands.node.Group;
import org.comroid.commands.node.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.*;
import static java.util.stream.Stream.*;
import static java.util.stream.Stream.of;
import static org.comroid.api.func.util.Streams.*;

@Value
@RequiredArgsConstructor
public class JdaCommandAdapter extends AbstractCommandAdapter implements PermissionChecker {
    private final CommandManager manager;
    Set<CommandCapability> capabilities = Set.of(CommandCapability.NAMED_ARGS);
    JDA                    jda;
    Event.Bus<GenericEvent> bus          = new Event.Bus<>();
    @Nullable @NonFinal @Setter BiFunction<EmbedBuilder, User, EmbedBuilder> embedFinalizer = null;
    @Setter @NonFinal           boolean                                      initialized    = false;
    @Setter @NonFinal boolean purgeCommands = false;//Debug.isDebug();

    @Override
    public void initialize() {
        if (initialized) return;

        manager.addChild(this);

        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onGenericEvent(@NotNull GenericEvent event) {
                bus.publish(event);
            }
        });
        bus.flatMap(SlashCommandInteractionEvent.class)
                .listen()
                .subscribeData(event -> manager.execute(JdaCommandAdapter.this,
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
            var options = manager.autoComplete(JdaCommandAdapter.this,
                            event.getCommandString().substring(1).split(" "),
                            option.getName(),
                            option.getValue(),
                            event.getName(),
                            event,
                            event.getUser(),
                            event.getMember(),
                            event.getGuild(),
                            event.getChannel())
                    .map(e -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(e.key(), e.description()))
                    .limit(25)
                    .toList();
            event.replyChoices(options).queue();
        });

        var helper = new Object() {
            public Stream<Call> expandToCallNodes(Node node) {
                return of(node).flatMap(it -> {
                    if (it instanceof Group group) return group.nodes().flatMap(this::expandToCallNodes);
                    return of(it).flatMap(cast(Call.class));
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

            for (var node : manager.getBaseNodes()) {
                if (!purgeCommands && existing.stream()
                        .map(ICommandReference::getName)
                        .anyMatch(node.getName()::equalsIgnoreCase)) continue;

                SlashCommandData cmd = Commands.slash(node.getName().toLowerCase(), node.getDescription());

                switch (node) {
                    case Group group -> {
                        for (var callable : group.nodes().toList()) {
                            if (callable instanceof Group g0) cmd.addSubcommandGroups(makeGroup(g0));
                            if (callable instanceof Call c0) cmd.addSubcommands(makeMember(c0));
                        }
                    }
                    case Call call -> {
                        var perm = call.getAttribute().permission();
                        if (perm.matches("\\d+")) cmd.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Long.parseLong(
                                perm)));
                        for (var parameter : call.getParameters()) {
                            cmd.addOption(optionType(parameter),
                                    parameter.name().toLowerCase(),
                                    parameter.getDescription(),
                                    parameter.isRequired(),
                                    !parameter.getAutoFillProviders().isEmpty());
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

    private SubcommandGroupData makeGroup(Group group) {
        var data = new SubcommandGroupData(group.name(), group.getDescription());
        for (var sub : group.nodes().toList())
            if (sub instanceof Call call) {
                var child = makeMember(call);
                data.addSubcommands(child);
            }
        return data;
    }

    private SubcommandData makeMember(Call call) {
        var data = new SubcommandData(call.name(), call.getDescription());
        for (var parameter : call.getParameters()) {
            data.addOption(optionType(parameter),
                    parameter.name().toLowerCase(),
                    parameter.getDescription(),
                    parameter.isRequired(),
                    !parameter.getAutoFillProviders().isEmpty());
        }
        return data;
    }

    private OptionType optionType(org.comroid.commands.node.Parameter parameter) {
        return Optional.of(parameter.getParam().getType()).flatMap(t -> {
            if (Boolean.class.isAssignableFrom(t)) return Optional.of(OptionType.BOOLEAN);
            if (Integer.class.isAssignableFrom(t) || Long.class.isAssignableFrom(t))
                return Optional.of(OptionType.INTEGER);
            if (Number.class.isAssignableFrom(t)) return Optional.of(OptionType.NUMBER);
            if (User.class.isAssignableFrom(t) || Member.class.isAssignableFrom(t)) return Optional.of(OptionType.USER);
            if (Channel.class.isAssignableFrom(t)) return Optional.of(OptionType.CHANNEL);
            if (Role.class.isAssignableFrom(t)) return Optional.of(OptionType.ROLE);
            if (IMentionable.class.isAssignableFrom(t)) return Optional.of(OptionType.MENTIONABLE);
            if (Message.Attachment.class.isAssignableFrom(t)) return Optional.of(OptionType.ATTACHMENT);
            return Optional.empty();
        }).orElse(OptionType.STRING);
    }

    private CompletableFuture<?> handleResponse(JdaCommandAdapter.MessageSender hook, User user, Object response) {
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
    public void handleResponse(CommandUsage cmd, @NotNull Object response, Object... args) {
        final var e         = of(args).flatMap(cast(SlashCommandInteractionEvent.class)).findAny().orElseThrow();
        final var user      = of(args).flatMap(cast(User.class)).findAny().orElseThrow();
        var       ephemeral = cmd.getStackTrace().peek().getAttribute().privacy() != CommandPrivacyLevel.PUBLIC;

        if (response instanceof CompletableFuture) e.deferReply()
                .setEphemeral(ephemeral)
                .submit()
                .thenCombine(((CompletableFuture<?>) response),
                        (hook, resp) -> handleResponse(msg -> hook.sendMessage(msg).submit(), user, resp))
                .thenCompose(identity())
                .exceptionally(Debug.exceptionLogger("Could not defer reply to command"));
        else handleResponse(msg -> e.reply(msg).setEphemeral(ephemeral).submit(),
                user,
                response).exceptionally(Debug.exceptionLogger("Could not reply to command"));
    }

    @Override
    public boolean acceptPermission(String key) {
        return key.matches("\\d+");
    }

    @Override
    public boolean userHasPermission(CommandUsage usage, Object key) {
        if (key == null || String.valueOf(key).isBlank()) return true;
        var permissions = Permission.getPermissions(Long.parseLong(key.toString()));
        return usage.getContext()
                .stream()
                .flatMap(cast(Member.class))
                .anyMatch(usr -> usr.getIdLong() == 141476933849448448L /* kaleidox is superadmin for testing purposes */ || usr.hasPermission(
                        permissions));
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum OptionAdapter implements JdaCommandAdapter.IOptionAdapter, Named {
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

        public static Wrap<JdaCommandAdapter.IOptionAdapter> of(final Class<?> type) {
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
        public class Enum<T> implements JdaCommandAdapter.IOptionAdapter {
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
                final var value = JdaCommandAdapter.OptionAdapter.this.getFrom(option);
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
            if (message == null || event.getMessageIdLong() != message.getIdLong() || event.getUser().isBot()) return;

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
