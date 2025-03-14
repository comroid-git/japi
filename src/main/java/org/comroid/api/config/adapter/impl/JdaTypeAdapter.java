package org.comroid.api.config.adapter.impl;

import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import org.comroid.api.config.adapter.TypeAdapter;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.func.ext.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Stream;

@Value
@NonFinal
public abstract class JdaTypeAdapter<T extends ISnowflake> extends TypeAdapter<T, @NotNull Long> {
    public static final JdaTypeAdapter<NewsChannel> NEWS_CHANNEL = new JdaTypeAdapter<>(NewsChannel.class) {
        @Override
        protected Stream<NewsChannel> findAll(JDA jda) {
            return jda.getNewsChannels().stream();
        }

        @Override
        public NewsChannel deserialize(Context context, @NotNull Long id) {
            return context.getFromContext(JDA.class, false).assertion().getNewsChannelById(id);
        }
    };

    public JdaTypeAdapter(Class<T> type) {
        super(type, StandardValueType.LONG, "id");
    }

    @Override
    public @NotNull Long toSerializable(Context context, T value) {
        return value.getIdLong();
    }

    protected abstract Stream<T> findAll(JDA jda);

    protected Stream<T> findAllById(JDA jda, long... id) {
        return findAll(jda).filter(i -> Arrays.binarySearch(id, i.getIdLong()) >= 0);
    }
}
