package org.comroid.interaction.adapter.jda;

import lombok.extern.java.Log;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.data.RegExpUtil;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.tree.Initializable;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.component.NameCapitalizer;
import org.comroid.interaction.node.GroupNode;
import org.comroid.interaction.node.MethodNode;
import org.comroid.interaction.node.ParameterNode;
import org.comroid.interaction.node.model.InteractionNode;
import org.comroid.interaction.node.model.InvokableNode;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.comroid.interaction.adapter.jda.JdaAdapter.*;

@Log
record DiscordCommandRegistrator(JdaAdapter adp) implements Initializable {
    /// register commands with discord
    @Override
    public void initialize() {
        var all = new ArrayList<CommandData>();

        for (var tree : adp.getCore().getRegistered()) {
            for (var base : tree.getNodes()) {
                CommandData data;
                var         ctx = base.getDefinitionValues(KEY_CONTEXT).findAny().orElse(CONTEXT_COMMAND);
                var cap = adp.component(NameCapitalizer.class).assertion();

                if (base instanceof GroupNode group) {
                    if (!ctx.equalsIgnoreCase(CONTEXT_COMMAND)) {
                        log.severe("Cannot register interaction with discord; groups are only allowed with slash commands; " + base);
                        continue;
                    }

                    // only slash commands can be grouped
                    data = Commands.slash(cap.getCapitalization(NameCapitalizer.ComponentType.CALLABLE).convert(base.getInteraction().value()),
                            base.getInteraction().getDescription() != null ? base.getInteraction().getDescription() : InteractionCore.NO_DESCRIPTION);

                    for (var child : group.getChildren()) {
                        var subcommand = createSubcommand(child);
                        ((SlashCommandData) data).addSubcommands(subcommand);
                    }
                } else if (base instanceof MethodNode method) data = switch (ctx) {
                    case CONTEXT_COMMAND -> {
                        var slash = Commands.slash(cap.getCapitalization(NameCapitalizer.ComponentType.CALLABLE).convert(base.getInteraction().value()),
                                base.getInteraction().getDescription() != null ? base.getInteraction().getDescription() : InteractionCore.NO_DESCRIPTION);

                        for (var param : method.getParameters()) slash.addOptions(createOptionData(param));

                        yield slash;
                    }
                    case CONTEXT_MESSAGE -> Commands.message(cap.getCapitalization(NameCapitalizer.ComponentType.TITLE).convert(base.getInteraction().value()));
                    case CONTEXT_USER -> Commands.user(cap.getCapitalization(NameCapitalizer.ComponentType.TITLE).convert(base.getInteraction().value()));
                    default -> throw new IllegalStateException("Unexpected value: " + ctx);
                };
                else throw new IllegalStateException("Unexpected value: " + base);

                initCommandData(base, data);
                all.add(data);
            }
        }

        var jda = adp.getJda();
        log.info("Upserting %d interactions to discord bot %s".formatted(all.size(), jda.getSelfUser()));

        RestAction<?> action = PURGE_COMMANDS.consume() ? jda.retrieveCommands().flatMap(cmds -> {
            log.fine("Puring %d previously defined commands".formatted(cmds.size()));

            RestAction<?> sub = new CompletedRestAction<>(jda, (Object) null);
            for (var cmd : cmds)
                sub = sub.flatMap($ -> jda.deleteCommandById(cmd.getId()));

            return sub;
        }) : new CompletedRestAction<>(jda, (Object) null);

        for (var data : all) action = action.flatMap($ -> jda.upsertCommand(data));

        action.queue();
    }

    static void initCommandData(InteractionNode node, CommandData data) {
        initDefaultPermission(node.getDefinitionValues(KEY_PERMISSION), data);
        initNsfw(node, data);
    }

    static void initDefaultPermission(Stream<String> permissions, CommandData data) {
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

    static void initNsfw(InteractionNode node, CommandData data) {
        data.setNSFW(node.getDefinitionValues(KEY_NSFW).anyMatch(str -> str.toLowerCase().matches(RegExpUtil.TRUE_PATTERN)));
    }

    static SubcommandData createSubcommand(InvokableNode node) {
        return switch (node) {
            case GroupNode ignored -> throw new IllegalStateException("GroupNode may not be a subcommand: " + node);
            case MethodNode method -> new SubcommandData(method.getInteraction().value(),
                    method.getInteraction().getDescription() != null ? method.getInteraction().getDescription() : InteractionCore.NO_DESCRIPTION).addOptions(
                    method.getParameters().stream().map(DiscordCommandRegistrator::createOptionData).toList());
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };
    }

    static @NonNull OptionData createOptionData(ParameterNode param) {
        return new OptionData(getOptionType(param),
                param.getParameter().value(),
                param.getParameter().getDescription() != null ? param.getParameter().getDescription() : InteractionCore.NO_DESCRIPTION,
                param.getParameter().required(),
                !param.getCompletion().isEmpty() || param.getReflect().getType().isEnum());
    }

    static OptionType getOptionType(ParameterNode node) {
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
}
