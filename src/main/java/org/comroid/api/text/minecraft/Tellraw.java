package org.comroid.api.text.minecraft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.With;
import org.comroid.api.attr.Named;
import org.comroid.api.data.Vector;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.adp.JSON;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.comroid.api.text.minecraft.McFormatCode.*;

public interface Tellraw {
    static Command.Builder notify(Object selector, Component title, String content) {
        return Command.builder()
                .selector(selector)
                .component(White.text("<").build())
                .component(title)
                .component(White.text("> ").build())
                .component(Reset.text(content).build());
    }

    @With
    @Value
    @Builder
    class Command implements Tellraw {
        Object selector;
        @Singular
        List<Component> components;

        @Override
        @SneakyThrows
        public String toString() {
            return "tellraw " + selector + " " + components.stream()
                    .map(Component::toString)
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    @With
    @Value
    @Builder
    class Selector {
        Base base;
        @Nullable Vector.N3 coordinate;
        @Nullable Double    distance;
        @Nullable Vector.N3 dimensions;
        @Nullable Double    Pitch;
        @Nullable Double    Yaw;
        @Nullable String    tag;
        @Nullable String    team;
        @Nullable String    name;
        @Nullable String    type;
        @Nullable String    predicate;
        @Nullable String    nbt;
        @Nullable Double    level;
        @Nullable String    gamemode;
        @Nullable @Singular List<String> scores;
        @Nullable @Singular List<String> advancements;

        @Override
        @SneakyThrows
        public String toString() {
            return base.string; // todo
        }

        @Getter
        public enum Base {
            NEAREST_PLAYER("@p"),
            RANDOM_PLAYER("@r"),
            ALL_PLAYERS("@a"),
            ALL_ENTITIES("@e"),
            EXECUTOR("@s");

            private final String string;

            Base(String string) {
                this.string = string;
            }

            @Override
            @SneakyThrows
            public String toString() {
                return string;
            }
        }
    }

    @With
    @Value
    @Builder
    class Component implements Tellraw {
        @Nullable String text;
        @Nullable @Singular("format")
        Set<McFormatCode> format;
        @Nullable Event clickEvent;
        @Nullable Event hoverEvent;

        public String toFullString() {
            return "[%s]".formatted(toString());
        }

        @Override
        @SneakyThrows
        public String toString() {
            return json().toString();
        }

        public JSON.Object json() {
            var json = new JSON.Object();
            if (text != null) json.set("text", text);
            if (format != null) for (var code : format) {
                if (code.isFormat())
                    json.set(code.name().toLowerCase(), true);
                else if (code.isColor())
                    json.set("color", code.name().toLowerCase());
                else if (code.isReset()) {
                    for (var format : McFormatCode.FORMATS)
                        json.set(format.name().toLowerCase(), false);
                    json.set("color", White.name().toLowerCase());
                }
            }
            if (clickEvent != null) json.put("clickEvent", clickEvent.json());
            if (hoverEvent != null) json.put("hoverEvent", hoverEvent.json());
            return json;
        }
    }

    @With
    @Value
    @Builder
    @AllArgsConstructor
    class Event implements Tellraw {
        @NotNull Action action;
        @NotNull String value;

        @Override
        @SneakyThrows
        public String toString() {
            return json().toString();
        }

        public JSON.Object json() {
            var json = new JSON.Object();
            json.set("action", action.name());
            if (action != Action.show_text)
                json.set("value", value);
            else json.set("contents", Arrays.stream(value.split("\n"))
                    .map(DataNode.Value::new)
                    .collect(Collectors.toCollection(JSON.Array::new)));
            return json;
        }

        @SuppressWarnings("unused")
        public enum Action implements Named {
            open_url,
            run_command,
            suggest_command,
            change_page,

            show_text,
            show_item,
            show_entity;

            public Event value(String value) {
                return new Event(this, value);
            }
        }
    }
}
