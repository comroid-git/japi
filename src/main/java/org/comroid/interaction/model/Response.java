package org.comroid.interaction.model;

import lombok.Builder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.api.model.ValidityCheck;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Experimental
@Builder(toBuilder = true)
public record Response(
        @Nullable String title,
        @Nullable String content,
        @Nullable String url,
        @Nullable String imageUrl,
        @Nullable String footnote,
        @Nullable Instant timestamp,
        @Nullable Color color,
        @Nullable Author author,
        @NonNull List<@NonNull Detail> details
) implements ValidityCheck {
    public static Response of(CharSequence chars) {
        return builder().content(chars.toString()).build();
    }

    public static Response of(EmbedBuilder embed) {
        return of(embed.build());
    }

    public static Response of(MessageEmbed embed) {
        return builder().title(embed.getTitle())
                .content(embed.getDescription())
                .url(embed.getUrl())
                .imageUrl(embed.getImage() != null ? embed.getImage().getProxyUrl() : null)
                .footnote(embed.getFooter() != null ? embed.getFooter().getText() : null)
                .timestamp(embed.getTimestamp() != null ? embed.getTimestamp().toInstant() : null)
                .color(embed.getColor())
                .author(embed.getAuthor() != null ? new Author(embed.getAuthor().getName(), embed.getAuthor().getProxyIconUrl()) : null)
                .details(embed.getFields()
                        .stream()
                        .filter(field -> field.getName() != null && field.getValue() != null)
                        .map(field -> new Detail(field.getName(), field.getValue()))
                        .toList())
                .build();
    }

    public Response(
            @Nullable String title, @Nullable String content, @Nullable String url, @Nullable String imageUrl, @Nullable String footnote,
            @Nullable Instant timestamp, @Nullable Color color, @Nullable Author author
    ) {
        this(title, content, url, imageUrl, footnote, timestamp, color, author, new ArrayList<>());
    }

    public boolean isPlaintext() {
        return content != null && details.isEmpty() && Stream.of(title, url, imageUrl, footnote, timestamp, color, author).allMatch(Objects::isNull);
    }

    public boolean isWebhookPlaintext() {
        return content != null && details.isEmpty() && Stream.of(title, url, imageUrl, footnote, timestamp, color).allMatch(Objects::isNull);
    }

    @Override
    public boolean isValid() {
        return !details.isEmpty() || Stream.of(title, content, url, imageUrl, footnote, color, author, timestamp).anyMatch(Objects::nonNull);
    }

    public EmbedBuilder toEmbed() {
        var embed = new EmbedBuilder().setTitle(title)
                .setDescription(content)
                .setUrl(url)
                .setImage(imageUrl)
                .setFooter(footnote)
                .setTimestamp(timestamp)
                .setColor(color);

        if (author != null && author.isValid()) embed.setAuthor(author.name(), null, author.iconUrl());
        else embed.setAuthor(null);

        for (var detail : details) embed.addField(detail.toField());

        return embed;
    }

    @lombok.Builder(toBuilder = true)
    public record Author(
            @Nullable String name, @Nullable String iconUrl
    ) implements ValidityCheck {
        public static Author of(UserSnowflake snowflake) {
            if (snowflake instanceof Member member) return new Author(member.getEffectiveName(), member.getEffectiveAvatarUrl());
            if (snowflake instanceof User user) return new Author(user.getName(), user.getAvatarUrl());
            return new Author(snowflake.getAsMention(), snowflake.getDefaultAvatarUrl());
        }

        public static Author of(MessageEmbed.AuthorInfo author) {
            return new Author(author.getName(), author.getProxyIconUrl());
        }

        @Override
        public boolean isValid() {
            return Stream.of(name, iconUrl).anyMatch(Objects::nonNull);
        }
    }

    @lombok.Builder(toBuilder = true)
    public record Detail(
            @NonNull String name, @NonNull String description
    ) implements ValidityCheck {
        public static Detail of(Named object) {
            return new Detail(object.getName(), object instanceof Described described ? described.getDescription() : object.toString());
        }

        public static Detail of(MessageEmbed.Field field) {
            return new Detail(Objects.requireNonNull(field.getName(), "field name"), Objects.requireNonNull(field.getValue(), "field value"));
        }

        @Override
        public boolean isValid() {
            return Stream.of(name, description).anyMatch(Objects::nonNull);
        }

        public MessageEmbed.Field toField() {
            return new MessageEmbed.Field(name, description, false);
        }
    }
}
