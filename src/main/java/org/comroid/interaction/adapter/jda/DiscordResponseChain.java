package org.comroid.interaction.adapter.jda;

import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.comroid.annotations.Instance;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.component.response.ResponseChain;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.model.Response;
import org.comroid.util.JdaUtil;
import org.jspecify.annotations.Nullable;

@Log
enum DiscordResponseChain implements ResponseChain<MessageCreateData> {
    @Instance INSTANCE;

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

    static Interaction.PrivacyLevel privacy(InteractionContext context) {
        return context.child(Interaction.class).map(Interaction::privacy).orElse(Interaction.PrivacyLevel.EPHEMERAL);
    }

    static boolean async(InteractionContext context) {
        return context.child(Interaction.class).filter(Interaction::async).isNonNull();
    }
}
