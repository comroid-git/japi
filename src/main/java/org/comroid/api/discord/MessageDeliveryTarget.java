package org.comroid.api.discord;

import lombok.AllArgsConstructor;
import lombok.Value;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public interface MessageDeliveryTarget {
    default RestAction<Message> send(String message) {
        return send(new MessageCreateBuilder().setContent(message).build());
    }

    RestAction<Message> send(MessageCreateData message);

    @Value
    class Channel implements MessageDeliveryTarget {
        MessageChannel channel;

        @Override
        public RestAction<Message> send(MessageCreateData message) {
            return channel.sendMessage(message);
        }
    }

    @Value
    class Reply implements MessageDeliveryTarget {
        Message target;

        @Override
        public RestAction<Message> send(MessageCreateData message) {
            return target.reply(message);
        }
    }

    @Value
    @AllArgsConstructor
    class ReplyCallback implements MessageDeliveryTarget {
        IReplyCallback callback;
        boolean        ephemeral;

        public ReplyCallback(IReplyCallback callback) {
            this(callback, true);
        }

        @Override
        public RestAction<Message> send(MessageCreateData message) {
            return callback.reply(message).setEphemeral(ephemeral).map(hook -> hook.getCallbackResponse().getMessage());
        }
    }

    @Value
    class Hook implements MessageDeliveryTarget {
        InteractionHook hook;

        @Override
        public WebhookMessageEditAction<Message> send(MessageCreateData message) {
            return hook.editOriginal(org.comroid.util.JdaUtil.convertToEditData(message));
        }
    }
}
