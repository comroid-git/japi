package org.comroid.api.config.adapter.impl;

import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.comroid.api.config.adapter.TypeAdapter;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.func.ext.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@Value
@NonFinal
public abstract class JdaTypeAdapter<T extends ISnowflake> extends TypeAdapter<T, @NotNull Long> {
    public static final JdaTypeAdapter<TextChannel>  TEXT_CHANNEL     = new JdaTypeAdapter<>(TextChannel.class) {
        @Override
        protected Stream<TextChannel> findAll(JDA jda) {
            return jda.getTextChannels().stream();
        }

        @Override
        public Optional<TextChannel> deserialize(Context context, @NotNull Long id) {
            return context.getFromContext(JDA.class, false).wrap().map(jda -> jda.getTextChannelById(id));
        }
    };
    public static final JdaTypeAdapter<ForumChannel> FORUM_CHANNEL    = new JdaTypeAdapter<>(ForumChannel.class) {
        @Override
        protected Stream<ForumChannel> findAll(JDA jda) {
            return jda.getForumChannels().stream();
        }

        @Override
        public Optional<ForumChannel> deserialize(Context context, @NotNull Long id) {
            return context.getFromContext(JDA.class, false).wrap().map(jda -> jda.getForumChannelById(id));
        }
    };
    public static final JdaTypeAdapter<NewsChannel>  NEWS_CHANNEL     = new JdaTypeAdapter<>(NewsChannel.class) {
        @Override
        protected Stream<NewsChannel> findAll(JDA jda) {
            return jda.getNewsChannels().stream();
        }

        @Override
        public Optional<NewsChannel> deserialize(Context context, @NotNull Long id) {
            return context.getFromContext(JDA.class, false).wrap().map(jda -> jda.getNewsChannelById(id));
        }
    };
    public static final JdaTypeAdapter<Category>     CHANNEL_CATEGORY = new JdaTypeAdapter<>(Category.class) {
        @Override
        protected Stream<Category> findAll(JDA jda) {
            return jda.getCategories().stream();
        }

        @Override
        public Optional<Category> deserialize(Context context, @NotNull Long id) {
            return context.getFromContext(JDA.class, false).wrap().map(jda -> jda.getCategoryById(id));
        }
    };

    public JdaTypeAdapter(Class<T> type) {
        super(type, StandardValueType.LONG, "id");
    }

    @Override
    public @NotNull Long toSerializable(Context context, T value) {
        return value.getIdLong();
    }

    @Override
    public @NotNull Long parseSerialized(@Nullable String str) {
        return str == null ? 0 : Long.parseLong(str);
    }

    protected abstract Stream<T> findAll(JDA jda);

    protected Stream<T> findAllById(JDA jda, long... id) {
        return findAll(jda).filter(i -> Arrays.binarySearch(id, i.getIdLong()) >= 0);
    }
}
